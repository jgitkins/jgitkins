package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import io.jgitkins.server.common.exception.JgitkinsException;

public class ApplicationException extends JgitkinsException {

    public ApplicationException(ApplicationErrorCode errorCode) {
        super(errorCode);
    }

    public ApplicationException(ApplicationErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ApplicationException(ApplicationErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ApplicationException(ApplicationProblemSpec problemSpec) {
        super(problemSpec);
    }

    public ApplicationException(ApplicationProblemSpec problemSpec, String message) {
        super(problemSpec, message);
    }

    public ApplicationException(ApplicationProblemSpec problemSpec, String message, Throwable cause) {
        super(problemSpec, message, cause);
    }
}
