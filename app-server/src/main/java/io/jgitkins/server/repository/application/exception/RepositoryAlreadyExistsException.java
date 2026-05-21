package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class RepositoryAlreadyExistsException extends ApplicationException {

    public RepositoryAlreadyExistsException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_ALREADY_EXISTS, message);
    }
}
