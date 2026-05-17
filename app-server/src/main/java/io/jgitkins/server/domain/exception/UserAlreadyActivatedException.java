package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainProblemSpec;

public class UserAlreadyActivatedException extends DomainException {

    public UserAlreadyActivatedException() {
        super(DomainProblemSpec.USER_ALREADY_ACTIVATED, "User is already activated");
    }
}
