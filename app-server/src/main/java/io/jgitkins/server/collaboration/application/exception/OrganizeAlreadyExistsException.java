package io.jgitkins.server.collaboration.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;
import io.jgitkins.server.application.exception.ApplicationException;

public class OrganizeAlreadyExistsException extends ApplicationException {

    public OrganizeAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.ORGANIZE_ALREADY_EXISTS, message);
    }
}
