package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class InfrastructureException extends JgitkinsException {

    public InfrastructureException(InfrastructureErrorCode errorCode) {
        super(errorCode);
    }

    public InfrastructureException(InfrastructureErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InfrastructureException(InfrastructureErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public InfrastructureException(InfrastructureProblemSpec problemSpec) {
        super(problemSpec);
    }

    public InfrastructureException(InfrastructureProblemSpec problemSpec, String message) {
        super(problemSpec, message);
    }

    public InfrastructureException(InfrastructureProblemSpec problemSpec, String message, Throwable cause) {
        super(problemSpec, message, cause);
    }
}
