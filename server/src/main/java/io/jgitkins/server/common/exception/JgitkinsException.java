package io.jgitkins.server.common.exception;

import io.jgitkins.server.common.error.ErrorCode;

public abstract class JgitkinsException extends RuntimeException {
    private final ErrorCode errorCode;

    public JgitkinsException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public JgitkinsException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public JgitkinsException(ErrorCode errorCode, String message, Throwable cause) {
        super((message == null || message.isBlank()) ? errorCode.getDefaultMessage() : message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
