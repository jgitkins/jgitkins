package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class RepositoryNotInitializedException extends ApplicationException {

    public RepositoryNotInitializedException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_NOT_INITIALIZED, message);
    }
}
