package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.internal.RepositoryKey;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

import java.util.List;
import java.util.Optional;

public interface RepositoryLoadUseCase {
    RepositoryResult loadRepository(Long repositoryId);

    /**
     * Resolves a repository id to its {@code namespace/repoName} key, for the ID-based upload route.
     *
     * <p>Task 2.64. The controller previously loaded the whole {@code RepositoryResult} and derived the
     * key itself, which meant an upload route depended on the full read contract — including the fields
     * task 2.65 governs — to learn two strings. This boundary returns only what the route needs.
     *
     * <p>Empty rather than throwing when the repository is missing or its paths are unusable, so the
     * caller keeps ownership of the 404 it already produces.
     */
    Optional<RepositoryKey> resolveRepositoryKey(Long repositoryId);
    RepositoryResult loadRepositoryByPath(String namespace, String repoName);
    List<RepositoryResult> loadRepositories();

    List<RepositoryResult> loadUserRepositories(String username);
}
