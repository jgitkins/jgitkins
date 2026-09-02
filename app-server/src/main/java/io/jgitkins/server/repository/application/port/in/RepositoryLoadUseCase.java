package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.RepositoryKey;
import io.jgitkins.server.repository.application.contract.RepositoryResult;

import java.util.List;
import java.util.Optional;

public interface RepositoryLoadUseCase {
    /**
     * @param requesterUserId the authenticated caller, or {@code null} for an anonymous request.
     *     Nullable on the read side and not on the write side, deliberately: a public repository is
     *     readable without a caller, and forcing a value here would either reject anonymous reads or
     *     invent a sentinel actor that the visibility filter would then have to recognise.
     */
    RepositoryResult loadRepository(Long requesterUserId, Long repositoryId);

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
    List<RepositoryResult> loadRepositories(Long requesterUserId);

    List<RepositoryResult> loadUserRepositories(Long requesterUserId, String username);
}
