package io.jgitkins.server.change.review.domain.model;

import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.CommitHash;

public record BranchHeadSnapshot(
        BranchName branchName,
        CommitHash commitHash
) {

    public BranchHeadSnapshot {
        if (branchName == null) {
            throw new IllegalArgumentException("Branch name must not be null");
        }
        if (commitHash == null) {
            throw new IllegalArgumentException("Commit hash must not be null");
        }
    }

    public static BranchHeadSnapshot of(String branchName, String commitHash) {
        return new BranchHeadSnapshot(BranchName.of(branchName), CommitHash.of(commitHash));
    }

    public boolean hasSameBranch(BranchHeadSnapshot other) {
        return other != null && branchName.equals(other.branchName);
    }
}
