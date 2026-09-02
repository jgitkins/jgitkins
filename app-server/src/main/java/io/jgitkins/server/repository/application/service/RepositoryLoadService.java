package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import java.util.Optional;
import io.jgitkins.server.repository.application.contract.result.RepositoryKey;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepositoryLoadService implements RepositoryLoadUseCase {

    private final RepositoryQueryPort repositoryQueryPort;
    private final UserNamespacePort userNamespacePort;
    private final RepositoryAccessValidator repositoryAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public RepositoryResult loadRepository(Long requesterUserId, Long repositoryId) {
        RepositoryResult repository = repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        // Authorized against the very result being returned, not a re-read. A second lookup could
        // observe a different row than the one about to leave this method, and would authorize state
        // the caller never sees.
        repositoryAccessValidator.validateReadAccess(repository, requesterUserId);
        return repository;
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
    public List<RepositoryResult> loadRepositories(Long requesterUserId) {
        // No validator call: the visibility filter is the authorization for a list, and running a
        // per-row check afterwards would authorize rows the query already excluded.
        return repositoryQueryPort.loadVisibleRepositories(requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepositoryResult> loadUserRepositories(Long requesterUserId, String username) {
        String normalizedUsername = username != null ? username.trim() : "";

        userNamespacePort.findUserIdByUsername(normalizedUsername)
                .orElseThrow(() -> new RepositoryNotFoundException("User not found: " + normalizedUsername));

        return repositoryQueryPort.loadUserRepositories(normalizedUsername, requesterUserId);
    }
}
