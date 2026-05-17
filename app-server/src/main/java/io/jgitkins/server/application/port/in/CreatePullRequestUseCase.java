package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.application.dto.result.PullRequestResult;

public interface CreatePullRequestUseCase {

    PullRequestResult createPullRequest(PullRequestCreateCommand command);
}
