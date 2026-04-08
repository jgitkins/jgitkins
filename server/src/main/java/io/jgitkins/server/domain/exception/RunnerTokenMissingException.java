package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainProblemSpec;

public class RunnerTokenMissingException extends DomainException {

    public RunnerTokenMissingException() {
        super(DomainProblemSpec.RUNNER_TOKEN_MISSING, "Runner activation token is required");
    }
}
