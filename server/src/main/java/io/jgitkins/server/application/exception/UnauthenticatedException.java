package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class UnauthenticatedException extends ApplicationException {

    public UnauthenticatedException() {
        super(ApplicationProblemSpec.UNAUTHENTICATED, "Unauthenticated");
    }

    public UnauthenticatedException(String message) {
        super(ApplicationProblemSpec.UNAUTHENTICATED, message);
    }
}
