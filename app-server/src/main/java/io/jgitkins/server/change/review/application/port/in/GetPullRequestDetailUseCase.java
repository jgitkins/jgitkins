package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;
import java.io.IOException;

public interface GetPullRequestDetailUseCase {

    PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId) throws IOException;
}
