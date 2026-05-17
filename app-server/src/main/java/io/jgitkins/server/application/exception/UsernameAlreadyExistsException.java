package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class UsernameAlreadyExistsException extends ApplicationException {

    public UsernameAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.USERNAME_ALREADY_EXISTS, message);
    }
}
