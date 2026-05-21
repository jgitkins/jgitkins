package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class InvalidNamespaceException extends ApplicationException {

    public InvalidNamespaceException(String message) {
        super(ApplicationProblemSpec.INVALID_NAMESPACE, message);
    }
}
