package io.jgitkins.server.change.review.domain.repository;

import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import java.util.Optional;

public interface PullRequestRepository {

    PullRequest save(PullRequest pullRequest);

    Optional<PullRequest> findById(PullRequestId id);
}
