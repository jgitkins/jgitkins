package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class RepositoryAccessDeniedException extends ApplicationException {

    public RepositoryAccessDeniedException(String message) {
        super(ApplicationProblemSpec.REPOSITORY_ACCESS_DENIED, message);
    }
}
