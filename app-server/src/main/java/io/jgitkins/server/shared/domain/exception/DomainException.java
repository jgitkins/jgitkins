package io.jgitkins.server.shared.domain.exception;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.shared.domain.error.DomainErrorCode;
import io.jgitkins.server.shared.domain.error.DomainProblemSpec;

public class DomainException extends JgitkinsException {

    public DomainException(DomainErrorCode errorCode) {
        super(errorCode);
    }

    public DomainException(DomainErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DomainException(DomainErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public DomainException(DomainProblemSpec problemSpec) {
        super(problemSpec);
    }

    public DomainException(DomainProblemSpec problemSpec, String message) {
        super(problemSpec, message);
    }

    public DomainException(DomainProblemSpec problemSpec, String message, Throwable cause) {
        super(problemSpec, message, cause);
    }
}
