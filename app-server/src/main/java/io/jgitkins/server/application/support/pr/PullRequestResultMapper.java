package io.jgitkins.server.application.support.pr;

import io.jgitkins.server.application.dto.result.PullRequestResult;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import org.springframework.stereotype.Component;

@Component
public class PullRequestResultMapper {

    public PullRequestResult toResult(PullRequest pullRequest) {
        return PullRequestResult.builder()
                .id(pullRequest.getId() != null ? pullRequest.getId().value() : null)
                .repositoryId(pullRequest.getRepositoryId().getValue())
                .source(pullRequest.getSource())
                .target(pullRequest.getTarget())
                .status(pullRequest.getStatus())
                .createdAt(pullRequest.getCreatedAt())
                .updatedAt(pullRequest.getUpdatedAt())
                .build();
    }
}
