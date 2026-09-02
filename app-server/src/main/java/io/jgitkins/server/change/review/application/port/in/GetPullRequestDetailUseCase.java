package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.contract.PullRequestDetailResult;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import java.io.IOException;

public interface GetPullRequestDetailUseCase {

    /**
     * @param requesterUserId nullable — a public repository's pull request is readable anonymously.
     */
    PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId, Long requesterUserId)
            throws IOException;
}
