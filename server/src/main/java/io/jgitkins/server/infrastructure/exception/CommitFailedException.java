package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class CommitFailedException extends InfrastructureException {

    public CommitFailedException(String message) {
        super(InfrastructureProblemSpec.COMMIT_FAILED, message);
    }

    public CommitFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.COMMIT_FAILED, message, cause);
    }
}
