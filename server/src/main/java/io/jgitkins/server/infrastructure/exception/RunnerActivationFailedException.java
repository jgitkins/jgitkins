package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class RunnerActivationFailedException extends InfrastructureException {

    public RunnerActivationFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.RUNNER_ACTIVATION_FAILED, message, cause);
    }
}
