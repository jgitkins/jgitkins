package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class UsernameAlreadyExistsException extends ApplicationException {

    public UsernameAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.USERNAME_ALREADY_EXISTS, message);
    }
}
