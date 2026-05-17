package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import java.io.IOException;

public interface GetPullRequestDetailUseCase {

    PullRequestDetailResult getPullRequestDetail(PullRequestId pullRequestId) throws IOException;
}
