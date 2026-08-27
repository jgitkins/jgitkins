package io.jgitkins.server.repository.application.service;

import java.util.Optional;
import io.jgitkins.server.repository.application.contract.internal.RepositoryKey;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryQueryPort repositoryQueryPort;
    private final RepositoryActorPort repositoryActorPort;
    private final UserNamespacePort userNamespacePort;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long repositoryId) {
        return repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RepositoryKey> resolveRepositoryKey(Long repositoryId) {
        // clonePath first, then path -- the same precedence the controller used, kept because the two
        // can differ for an organization-owned repository and clonePath is the one git actually serves.
        return repositoryQueryPort.loadRepository(repositoryId)
                .map(result -> {
                    RepositoryKey key = RepositoryKey.fromPath(result.clonePath());
                    return key != null ? key : RepositoryKey.fromPath(result.path());
                })
                .filter(java.util.Objects::nonNull);
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
        Long requesterId = repositoryActorPort.resolveCurrentUserId().orElse(null);
        return repositoryQueryPort.loadVisibleRepositories(requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadUserRepositories(String username) {
        String normalizedUsername = username != null ? username.trim() : "";

        userNamespacePort.findUserIdByUsername(normalizedUsername)
                .orElseThrow(() -> new RepositoryNotFoundException("User not found: " + normalizedUsername));

        Long requesterId = repositoryActorPort.resolveCurrentUserId().orElse(null);
        return repositoryQueryPort.loadUserRepositories(normalizedUsername, requesterId);
    }
}
