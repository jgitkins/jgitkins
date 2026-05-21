package io.jgitkins.server.change.review.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;

public class PullRequestNotFoundException extends ApplicationException {

    public PullRequestNotFoundException(PullRequestId pullRequestId) {
        super(ApplicationErrorCode.NOT_FOUND, "Pull request not found: " + pullRequestId);
    }
}
