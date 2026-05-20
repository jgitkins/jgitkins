package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class CommitLoadFailedException extends InfrastructureException {

    public CommitLoadFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.COMMIT_LOAD_FAILED, message, cause);
    }
}
