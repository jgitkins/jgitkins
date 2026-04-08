package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class CommitLoadFailedException extends InfrastructureException {

    public CommitLoadFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.COMMIT_LOAD_FAILED, message, cause);
    }
}
