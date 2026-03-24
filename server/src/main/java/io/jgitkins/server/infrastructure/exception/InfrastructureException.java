package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.common.exception.JgitkinsException;

public class InfrastructureException extends JgitkinsException {

    public InfrastructureException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InfrastructureException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InfrastructureException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
