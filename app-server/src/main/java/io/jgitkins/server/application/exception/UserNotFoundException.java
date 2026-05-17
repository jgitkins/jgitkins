package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

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
