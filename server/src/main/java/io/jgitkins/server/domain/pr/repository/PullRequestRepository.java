package io.jgitkins.server.domain.pr.repository;

import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;
import java.util.Optional;

public interface PullRequestRepository {

    PullRequest save(PullRequest pullRequest);

    Optional<PullRequest> findById(PullRequestId id);
}
