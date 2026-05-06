package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.application.exception.UserNotFoundException;
import io.jgitkins.server.application.mapper.RepositoryApplicationMapper;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.shared.application.support.RepositoryAccessibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryApplicationMapper repositoryApplicationMapper;
    private final RepositoryAccessibilityService repositoryAccessibilityService;
    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryQueryPort repositoryQueryPort;
    private final CurrentUserPort currentUserPersistencePort;
    private final UserPersistencePort userPort;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long repositoryId) {
        Repository repository = repositoryQueryPort.findById(RepositoryId.of(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        return repositoryApplicationMapper.toDto(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepositoryByPath(String namespace, String repoName) {
        Repository repository = repositoryLookupService.resolveByPath(namespace, repoName)
                .orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
        return repositoryApplicationMapper.toDto(repository);
    }

    // TODO: 개선 필요
    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadRepositories() {
        Optional<Long> requesterId = currentUserPersistencePort.resolveCurrentUserId();
        Map<OrganizeId, Boolean> membershipCache = new HashMap<>();

        return repositoryQueryPort.findAll().stream()
                .filter(repo -> repositoryAccessibilityService.isVisibleToRequester(repo, requesterId, membershipCache))
                .map(repositoryApplicationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadUserRepositories(String username) {
        String normalizedUsername = username != null ? username.trim() : "";

        Long ownerId = userPort.findUserIdByUsername(normalizedUsername)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + normalizedUsername));

        Optional<Long> requesterId = currentUserPersistencePort.resolveCurrentUserId();
        return repositoryQueryPort.findAllByOwner(OwnerType.USER, OwnerId.of(ownerId)).stream()
                .filter(repo -> repositoryAccessibilityService.isVisibleToUserOwner(repo, requesterId, ownerId))
                .map(repositoryApplicationMapper::toDto)
                .toList();
    }
}
