package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.contract.MergeResult;
import java.io.IOException;

public interface MergeabilityCheckUseCase {

    /**
     * @param requesterUserId nullable — a public repository's branch diff is readable anonymously.
     *     Before task P0a there was no such parameter and no check, so the diff between any two
     *     branches of any private repository was readable by anyone who knew its name.
     */
    MergeResult checkMergeability(String namespace, String repoName, String sourceBranch,
            String targetBranch, Long requesterUserId) throws IOException;
}
