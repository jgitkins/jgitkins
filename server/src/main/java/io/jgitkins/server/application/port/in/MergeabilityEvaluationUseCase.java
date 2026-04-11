package io.jgitkins.server.application.port.in;

import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import java.io.IOException;

public interface MergeabilityEvaluationUseCase {

    MergeabilityAssessment evaluateMergeability(String namespace, String repoName, String sourceBranch, String targetBranch)
            throws IOException;
}
