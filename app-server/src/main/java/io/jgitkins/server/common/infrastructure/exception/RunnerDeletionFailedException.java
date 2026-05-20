package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class RunnerDeletionFailedException extends InfrastructureException {

    public RunnerDeletionFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.RUNNER_DELETION_FAILED, message, cause);
    }
}
