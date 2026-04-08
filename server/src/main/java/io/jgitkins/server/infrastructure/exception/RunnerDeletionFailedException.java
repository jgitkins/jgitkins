package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class RunnerDeletionFailedException extends InfrastructureException {

    public RunnerDeletionFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.RUNNER_DELETION_FAILED, message, cause);
    }
}
