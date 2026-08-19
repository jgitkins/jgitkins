package io.jgitkins.server.change.review.application.port.out;

import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;

public interface BranchHeadPort {
    BranchHeadSnapshot getCurrentHead(ReviewRepositoryReference repository, String branchName);
}
