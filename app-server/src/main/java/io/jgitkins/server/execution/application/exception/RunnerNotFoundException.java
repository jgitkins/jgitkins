package io.jgitkins.server.execution.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import io.jgitkins.server.application.exception.ApplicationException;

public class RunnerNotFoundException extends ApplicationException {

    public RunnerNotFoundException() {
        super(ApplicationProblemSpec.RUNNER_NOT_FOUND, "Runner not found");
    }

    public RunnerNotFoundException(Long runnerId) {
        super(ApplicationProblemSpec.RUNNER_NOT_FOUND, "Runner not found: " + runnerId);
    }

    public RunnerNotFoundException(String message) {
        super(ApplicationProblemSpec.RUNNER_NOT_FOUND, message);
    }
}
