package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.BranchSearchResult;

import java.util.List;

public interface BranchLoadUseCase {

    /**
     * @param requesterUserId nullable. A public repository's branch list is readable anonymously; the
     *     visibility rule decides. Before task P0a these two methods took only the id, so a caller
     *     could enumerate the branches of any private repository.
     */
    List<BranchSearchResult> loadBranches(Long repositoryId, Long requesterUserId);

    /** @param requesterUserId nullable, same rule as {@link #loadBranches}. */
    BranchSearchResult loadBranch(Long repositoryId, String branchName, Long requesterUserId);
}
