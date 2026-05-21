package io.jgitkins.server.identity.access.domain.exception;

import io.jgitkins.server.shared.domain.error.DomainProblemSpec;
import io.jgitkins.server.shared.domain.exception.DomainException;

public class UserAlreadyActivatedException extends DomainException {

    public UserAlreadyActivatedException() {
        super(DomainProblemSpec.USER_ALREADY_ACTIVATED, "User is already activated");
    }
}
