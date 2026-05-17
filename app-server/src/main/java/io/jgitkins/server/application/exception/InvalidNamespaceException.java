package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class InvalidNamespaceException extends ApplicationException {

    public InvalidNamespaceException(String message) {
        super(ApplicationProblemSpec.INVALID_NAMESPACE, message);
    }
}
