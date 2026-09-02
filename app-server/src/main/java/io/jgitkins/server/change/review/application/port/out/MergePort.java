package io.jgitkins.server.change.review.application.port.out;

import io.jgitkins.server.change.review.application.contract.MergeRequest;
import io.jgitkins.server.change.review.application.contract.MergeResult;
import java.io.IOException;

public interface MergePort {
    MergeResult previewMergeability(String namespace, String repoName, String sourceBranch, String targetBranch) throws IOException;
    MergeResult merge(String namespace, String repoName, MergeRequest request) throws IOException;
}
