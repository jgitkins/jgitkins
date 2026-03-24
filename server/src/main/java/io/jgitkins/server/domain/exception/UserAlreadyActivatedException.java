package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainErrorCode;

public class UserAlreadyActivatedException extends DomainException {

    public UserAlreadyActivatedException() {
        super(DomainErrorCode.USER_ALREADY_ACTIVATED, "User is already activated");
    }
}
