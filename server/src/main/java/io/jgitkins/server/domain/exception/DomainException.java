package io.jgitkins.server.domain.exception;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.common.exception.JgitkinsException;

public class DomainException extends JgitkinsException {

    public DomainException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
