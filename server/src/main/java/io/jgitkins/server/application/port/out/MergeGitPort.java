package io.jgitkins.server.application.port.out;

import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;

import java.io.IOException;

public interface MergeGitPort {
    MergeResult merge(String namespace, String repoName, MergeRequest request) throws IOException;
    MergeResult previewMerge(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException;

}
