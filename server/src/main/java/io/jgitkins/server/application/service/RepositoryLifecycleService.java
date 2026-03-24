package io.jgitkins.server.application.service;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.common.event.DomainEventPublisher;
import io.jgitkins.server.application.dto.command.RepositoryCreateCommand;
import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.server.application.port.in.RepositoryDeleteUseCase;
import io.jgitkins.server.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.RepositoryGitPort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.application.common.RepositoryPathHelper;
import io.jgitkins.server.application.support.RepositoryLookupService;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.validate.RepositoryValidator;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryLifecycleService implements RepositoryCreateUseCase,
        RepositoryLoadUseCase,
        RepositoryDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final DomainEventPublisher domainEventPublisher;

    private final RepositoryGitPort repositoryGitPort;
    private final RepositoryPersistencePort repositoryPort;
    private final CurrentUserPort currentUserPersistencePort;
    private final UserPersistencePort userPort;

    private final RepositoryValidator repositoryValidator;
    private final RepositoryLookupService repositoryLookupService;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        Repository repository = createRepository(command);
        validateRepositoryCreation(repository, command.organizeId());

        Repository saved = repositoryPort.save(repository);
        repositoryGitPort.initialize(
                repositoryNamespaceResolver.resolve(repository.getOwnerType(), repository.getOwnerId()),
                repository.getName().getValue());

        publishDomainEvents(saved);
        return repositoryApplicationMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult getRepository(Long repositoryId) {
        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                        "Repository not found: " + repositoryId));
        return repositoryApplicationMapper.toDto(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult getRepositoryByPath(String namespace, String repoName) {
        Repository repository = repositoryLookupService.findByPath(namespace, repoName)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                        String.format("Repository not found: %s/%s", namespace, repoName)));
        return repositoryApplicationMapper.toDto(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> getRepositories() {
        Optional<Long> requesterId = currentUserPersistencePort.resolveCurrentUserId();
        Map<OrganizeId, Boolean> membershipCache = new HashMap<>();

        return repositoryPort.findAll().stream()
                .filter(repo -> repositoryLookupService.isVisibleToRequester(repo, requesterId,
                        membershipCache))
                .map(repositoryApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> getRepositoriesByUsername(String username) {
        // TODO: Presentation 계층으로 이관 (Validator 통해 처리하기)
        String normalizedUsername = username != null ? username.trim() : "";

        Long ownerId = userPort.findUserIdByUsername(normalizedUsername)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.USER_NOT_FOUND,
                        "User not found: " + normalizedUsername));

        Optional<Long> requesterId = currentUserPersistencePort.resolveCurrentUserId();
        return repositoryPort.findAllByOwner(OwnerType.USER, OwnerId.of(ownerId)).stream()
                .filter(repo -> repositoryLookupService.isVisibleToUserOwner(repo, requesterId,
                        ownerId))
                .map(repositoryApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteRepository(Long repositoryId) {
        RepositoryId id = RepositoryId.of(repositoryId);
        Repository repository = repositoryPort.findById(id)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                        "Repository not found: " + repositoryId));

        repositoryValidator.enforceDeletionPermission(repository);

        String namespace = repositoryNamespaceResolver.resolve(repository);
        repositoryGitPort.deleteRepository(namespace, repository.getName().getValue());

        repositoryPort.deleteById(id);
    }

    private OwnerId resolveOwnerId(OwnerType ownerType, Long organizeId) {
        return ownerType == OwnerType.ORGANIZATION ? OwnerId.of(organizeId)
                : OwnerId.of(repositoryValidator.requireCurrentUserId());
    }

    private Repository createRepository(RepositoryCreateCommand command) {
        OwnerType ownerType = command.ownerType();
        OwnerId ownerId = resolveOwnerId(ownerType, command.organizeId());
        String namespace = repositoryNamespaceResolver.resolve(ownerType, ownerId);

        return Repository.create(
                ownerType,
                ownerId,
                RepositoryName.from(command.repoName()),
                RepositoryPath.from(command.repoName()),
                BranchName.of(command.mainBranch()),
                command.visibility() != null ? command.visibility() : RepositoryVisibility.PRIVATE,
                command.description(),
                RepositoryPathHelper.buildClonePath(namespace, command.repoName()),
                command.credentialId(),
                InitialCommitOptions.of(
                        command.readme(),
                        command.message(),
                        command.authorName(),
                        command.authorEmail()
                )
        );
    }

    private void validateRepositoryCreation(Repository repository, Long organizeId) {
        repositoryValidator.validateOwnership(repository.getOwnerType(), organizeId);
        repositoryValidator.validateRepositoryNameUnique(
                repository.getOwnerType(),
                repository.getOwnerId(),
                repository.getName());
    }

    private void publishDomainEvents(Repository repository) {
        domainEventPublisher.publish(repository.getDomainEvents());
        repository.clearDomainEvents();
    }
}
