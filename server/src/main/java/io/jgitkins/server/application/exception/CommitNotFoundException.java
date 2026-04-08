package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class CommitNotFoundException extends ApplicationException {

    public CommitNotFoundException(String commitHash) {
        super(ApplicationProblemSpec.COMMIT_NOT_FOUND, "Commit not found: " + commitHash);
    }

    public CommitNotFoundException(String message, boolean rawMessage) {
        super(ApplicationProblemSpec.COMMIT_NOT_FOUND, message);
    }
}
