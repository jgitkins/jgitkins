package io.jgitkins.server.presentation.exception;

import io.jgitkins.server.common.error.ErrorCode;
import io.jgitkins.server.common.exception.JgitkinsException;

public class PresentationException extends JgitkinsException {
    public PresentationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PresentationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PresentationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
