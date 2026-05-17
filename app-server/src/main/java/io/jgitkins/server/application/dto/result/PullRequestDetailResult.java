package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PullRequestDetailResult {

    private final Long id;
    private final Long repositoryId;
    private final BranchHeadSnapshot storedSource;
    private final BranchHeadSnapshot storedTarget;
    private final BranchHeadSnapshot currentSource;
    private final BranchHeadSnapshot currentTarget;
    private final PullRequestStatus status;
    private final TargetDrift targetDrift;
    private final MergeabilityAssessment mergeability;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
