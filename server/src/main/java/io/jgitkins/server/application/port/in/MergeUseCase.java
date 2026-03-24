package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;

import java.io.IOException;

public interface MergeUseCase {
    MergeResult performMerge(String namespace, String repoName, MergeRequest request) throws IOException;
}
