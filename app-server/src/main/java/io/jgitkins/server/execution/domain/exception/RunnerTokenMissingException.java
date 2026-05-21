package io.jgitkins.server.execution.domain.exception;

import io.jgitkins.server.shared.domain.error.DomainProblemSpec;
import io.jgitkins.server.shared.domain.exception.DomainException;

public class RunnerTokenMissingException extends DomainException {

    public RunnerTokenMissingException() {
        super(DomainProblemSpec.RUNNER_TOKEN_MISSING, "Runner activation token is required");
    }
}
