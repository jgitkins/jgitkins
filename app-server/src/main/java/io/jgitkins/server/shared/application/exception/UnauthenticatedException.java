package io.jgitkins.server.shared.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class UnauthenticatedException extends ApplicationException {

    public UnauthenticatedException() {
        super(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated");
    }

    public UnauthenticatedException(String message) {
        super(ApplicationProblemSpec.UNAUTHENTICATED, message);
    }
}
