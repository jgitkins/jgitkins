package io.jgitkins.server.common.presentation.exception;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.common.presentation.error.PresentationErrorCode;
import io.jgitkins.server.common.presentation.error.PresentationProblemSpec;

public class PresentationException extends JgitkinsException {
    public PresentationException(PresentationErrorCode errorCode) {
        super(errorCode);
    }

    public PresentationException(PresentationErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PresentationException(PresentationErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public PresentationException(PresentationProblemSpec problemSpec) {
        super(problemSpec);
    }

    public PresentationException(PresentationProblemSpec problemSpec, String message) {
        super(problemSpec, message);
    }

    public PresentationException(PresentationProblemSpec problemSpec, String message, Throwable cause) {
        super(problemSpec, message, cause);
    }
}
