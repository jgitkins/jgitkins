package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class RunnerActivationFailedException extends InfrastructureException {

    public RunnerActivationFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.RUNNER_ACTIVATION_FAILED, message, cause);
    }
}
