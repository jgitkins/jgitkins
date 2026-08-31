package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;

public interface CreatePullRequestUseCase {

    /**
     * @param requesterUserId required. Opening a pull request is a write against the repository, so
     *     it answers to the same rule as committing. Before task P0a this method took no requester
     *     and performed no check, so an anonymous caller could open a pull request on any repository
     *     including a private one.
     */
    PullRequestResult createPullRequest(PullRequestCreateCommand command, Long requesterUserId);
}
