package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class RepositoryAlreadyExistsException extends ApplicationException {

    public RepositoryAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_ALREADY_EXISTS, message);
    }
}
