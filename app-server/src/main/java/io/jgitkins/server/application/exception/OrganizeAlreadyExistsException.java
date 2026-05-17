package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class OrganizeAlreadyExistsException extends ApplicationException {

    public OrganizeAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.ORGANIZE_ALREADY_EXISTS, message);
    }
}
