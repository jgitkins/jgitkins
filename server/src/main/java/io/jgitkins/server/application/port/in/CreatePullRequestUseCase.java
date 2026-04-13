package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.application.dto.result.PullRequestResult;
import java.io.IOException;

public interface CreatePullRequestUseCase {

    PullRequestResult createPullRequest(PullRequestCreateCommand command) throws IOException;
}
