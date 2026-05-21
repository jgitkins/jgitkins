package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.identity.access.application.exception.UserNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.identity.access.application.port.out.CurrentUserPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryQueryPort repositoryQueryPort;
    private final CurrentUserPort currentUserPort;
    private final UserPersistencePort userPort;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long repositoryId) {
        return repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepositoryByPath(String namespace, String repoName) {
        return repositoryQueryPort.loadRepositoryByPath(namespace, repoName)
                .orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadRepositories() {
        Long requesterId = currentUserPort.resolveCurrentUserId().orElse(null);
        return repositoryQueryPort.loadVisibleRepositories(requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadUserRepositories(String username) {
        String normalizedUsername = username != null ? username.trim() : "";

        userPort.findUserIdByUsername(normalizedUsername)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + normalizedUsername));

        Long requesterId = currentUserPort.resolveCurrentUserId().orElse(null);
        return repositoryQueryPort.loadUserRepositories(normalizedUsername, requesterId);
    }
}
