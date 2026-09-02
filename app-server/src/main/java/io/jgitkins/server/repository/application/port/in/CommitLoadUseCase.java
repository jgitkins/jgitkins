package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.CommitHistory;

import java.util.List;

public interface CommitLoadUseCase {

    /**
     * @param requesterUserId nullable. A public repository's history is readable anonymously, so this
     *     is not a "must be logged in" parameter -- it is the identity the visibility rule is
     *     evaluated against. Before task P0a there was no such parameter and no rule: the namespace
     *     and name went straight to git, so any caller could read the full commit history and commit
     *     contents of any private repository by knowing its name.
     */
    CommitHistory getCommit(String namespace, String repoName, String commitHash, Long requesterUserId);

    /** @param requesterUserId nullable, same rule as {@link #getCommit}. */
    List<CommitHistory> getCommits(String namespace, String repoName, String branch, Long requesterUserId);
}
