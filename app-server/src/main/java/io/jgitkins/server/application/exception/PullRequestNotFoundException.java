package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;

public class PullRequestNotFoundException extends ApplicationException {

    public PullRequestNotFoundException(PullRequestId pullRequestId) {
        super(ApplicationErrorCode.NOT_FOUND, "Pull request not found: " + pullRequestId);
    }
}
