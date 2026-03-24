package io.jgitkins.server.application.exception;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.common.exception.JgitkinsException;

public class ApplicationException extends JgitkinsException {

    public ApplicationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ApplicationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ApplicationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
