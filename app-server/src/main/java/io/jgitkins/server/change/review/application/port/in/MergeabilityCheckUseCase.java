package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import java.io.IOException;

public interface MergeabilityCheckUseCase {

    MergeResult checkMergeability(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException;
}
