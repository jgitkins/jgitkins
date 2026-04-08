package io.jgitkins.server.application.exception;

import io.jgitkins.server.application.common.error.ApplicationProblemSpec;

public class RepositoryAccessDeniedException extends ApplicationException {

    public RepositoryAccessDeniedException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_ACCESS_DENIED, message);
    }
}
