package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

public class OrganizeAlreadyExistsException extends ApplicationException {

    public OrganizeAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.ORGANIZE_ALREADY_EXISTS, message);
    }
}
