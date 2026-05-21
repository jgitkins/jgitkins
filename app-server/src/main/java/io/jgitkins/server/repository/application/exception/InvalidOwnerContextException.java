package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class InvalidOwnerContextException extends ApplicationException {

    public InvalidOwnerContextException(String message) {
        super(ApplicationProblemSpec.INVALID_OWNER_CONTEXT, message);
    }
}
