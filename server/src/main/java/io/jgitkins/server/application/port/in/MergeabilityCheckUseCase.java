package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.MergeResult;

import java.io.IOException;

public interface MergeabilityCheckUseCase {
    MergeResult checkMergeability(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException;
}
