package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import java.io.IOException;

public interface MergeUseCase {

    MergeResult performMerge(String namespace, String repoName, MergeRequest request) throws IOException;
}
