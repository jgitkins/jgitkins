package io.jgitkins.server.application.service;

import io.jgitkins.server.application.common.RepositoryPathHelper;
import io.jgitkins.server.application.dto.command.RepositoryCreateCommand;
import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.server.application.port.in.RepositoryDeleteUseCase;
import io.jgitkins.server.application.port.out.RepositoryGitPort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.application.support.RepositoryProvisioner;
import io.jgitkins.server.application.validate.RepositoryValidator;
import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepositoryManagementService implements RepositoryCreateUseCase, RepositoryDeleteUseCase {

    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryProvisioner repositoryProvisioner;
    private final RepositoryGitPort repositoryGitPort;
    private final RepositoryPersistencePort repositoryPort;
    private final RepositoryValidator repositoryValidator;

    @Override
    @Transactional
    public RepositoryResult create(RepositoryCreateCommand command) {
        Repository repository = createRepository(command);
        validateRepositoryCreation(repository, command.organizeId());

        Repository saved = repositoryPort.save(repository);
        Repository provisioned = repositoryProvisioner.provision(saved, createInitialCommitOptions(command));
        return repositoryApplicationMapper.toDto(provisioned);
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
        InitialCommitOptions initialCommitOptions = createInitialCommitOptions(command);

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
                initialCommitOptions
        );
    }

    private InitialCommitOptions createInitialCommitOptions(RepositoryCreateCommand command) {
        return InitialCommitOptions.of(
                command.readme(),
                command.message(),
                command.authorName(),
                command.authorEmail()
        );
    }

    private void validateRepositoryCreation(Repository repository, Long organizeId) {
        repositoryValidator.validateOwnership(repository.getOwnerType(), organizeId);
        repositoryValidator.validateRepositoryNameUnique(
                repository.getOwnerType(),
                repository.getOwnerId(),
                repository.getName());
    }
}
