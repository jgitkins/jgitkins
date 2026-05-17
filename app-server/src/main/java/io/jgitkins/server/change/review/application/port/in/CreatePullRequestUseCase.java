package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;

public interface CreatePullRequestUseCase {

    PullRequestResult createPullRequest(PullRequestCreateCommand command);
}
