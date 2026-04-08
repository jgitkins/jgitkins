package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainProblemSpec;

public class RunnerTokenMismatchException extends DomainException {

    public RunnerTokenMismatchException() {
        super(DomainProblemSpec.RUNNER_TOKEN_MISMATCH, "Runner token does not match activation request");
    }
}
