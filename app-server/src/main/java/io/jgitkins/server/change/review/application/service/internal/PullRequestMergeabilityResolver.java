package io.jgitkins.server.change.review.application.service.internal;

import io.jgitkins.server.change.review.application.contract.MergeResult;
import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.MergePort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class PullRequestMergeabilityResolver {
    private final BranchHeadPort branchHeadPort;
    private final MergePort mergePort;
    private final MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;

    public BranchHeadSnapshot currentSourceHead(ReviewRepositoryReference repository, PullRequest pullRequest) {
        return branchHeadPort.getCurrentHead(repository, pullRequest.getSource().branchName().getValue());
    }
    public BranchHeadSnapshot currentTargetHead(ReviewRepositoryReference repository, PullRequest pullRequest) {
        return branchHeadPort.getCurrentHead(repository, pullRequest.getTarget().branchName().getValue());
    }
    public MergeabilityAssessment assess(ReviewRepositoryReference repository, PullRequest pullRequest) throws IOException {
        MergeResult result = mergePort.previewMergeability(repository.namespace(), repository.repoName(),
                pullRequest.getSource().branchName().getValue(), pullRequest.getTarget().branchName().getValue());
        return mergeabilityAssessmentAssembler.toAssessment(result);
    }
}
