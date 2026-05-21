package io.jgitkins.server.identity.access.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException() {
        super(ApplicationProblemSpec.USER_NOT_FOUND, "User not found");
    }

    public UserNotFoundException(Long userId) {
        super(ApplicationProblemSpec.USER_NOT_FOUND, "User not found: " + userId);
    }

    public UserNotFoundException(String message) {
        super(ApplicationProblemSpec.USER_NOT_FOUND, message);
    }
}
