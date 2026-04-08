package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class RepositoryNotInitializedException extends ApplicationException {

    public RepositoryNotInitializedException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_NOT_INITIALIZED, message);
    }
}
