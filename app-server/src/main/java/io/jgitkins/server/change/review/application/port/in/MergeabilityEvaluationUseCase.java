package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import java.io.IOException;

public interface MergeabilityEvaluationUseCase {

    MergeabilityAssessment evaluateMergeability(String namespace, String repoName, String sourceBranch, String targetBranch)
            throws IOException;
}
