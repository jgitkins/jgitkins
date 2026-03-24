package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainErrorCode;

public class RunnerTokenMismatchException extends DomainException {

    public RunnerTokenMismatchException() {
        super(DomainErrorCode.RUNNER_TOKEN_INVALID, "Runner token does not match activation request");
    }
}
