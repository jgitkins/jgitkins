package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainErrorCode;

public class RunnerTokenMissingException extends DomainException {

    public RunnerTokenMissingException() {
        super(DomainErrorCode.RUNNER_TOKEN_INVALID, "Runner activation token is required");
    }
}
