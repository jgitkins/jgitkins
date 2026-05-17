package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class InvalidOwnerContextException extends ApplicationException {

    public InvalidOwnerContextException(String message) {
        super(ApplicationProblemSpec.INVALID_OWNER_CONTEXT, message);
    }
}
