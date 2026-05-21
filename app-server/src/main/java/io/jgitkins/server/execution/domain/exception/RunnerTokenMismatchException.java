package io.jgitkins.server.execution.domain.exception;

import io.jgitkins.server.shared.domain.error.DomainProblemSpec;
import io.jgitkins.server.shared.domain.exception.DomainException;

public class RunnerTokenMismatchException extends DomainException {

    public RunnerTokenMismatchException() {
        super(DomainProblemSpec.RUNNER_TOKEN_MISMATCH, "Runner token does not match activation request");
    }
}
