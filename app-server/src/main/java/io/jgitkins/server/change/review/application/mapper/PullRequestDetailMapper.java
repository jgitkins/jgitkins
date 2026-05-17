package io.jgitkins.server.change.review.application.mapper;

import io.jgitkins.server.change.review.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.BranchHeadSnapshot;
import org.springframework.stereotype.Component;

@Component
public class PullRequestDetailMapper {

    public PullRequestDetailResult toDetail(PullRequest pullRequest,
                                            BranchHeadSnapshot currentSource,
                                            BranchHeadSnapshot currentTarget,
                                            MergeabilityAssessment mergeability) {
        return PullRequestDetailResult.builder()
                .id(pullRequest.getId() != null ? pullRequest.getId().value() : null)
                .repositoryId(pullRequest.getRepositoryId().getValue())
                .storedSource(pullRequest.getSource())
                .storedTarget(pullRequest.getTarget())
                .currentSource(currentSource)
                .currentTarget(currentTarget)
                .status(pullRequest.getStatus())
                .targetDrift(pullRequest.getTargetDrift())
                .mergeability(mergeability)
                .createdAt(pullRequest.getCreatedAt())
                .updatedAt(pullRequest.getUpdatedAt())
                .build();
    }
}
