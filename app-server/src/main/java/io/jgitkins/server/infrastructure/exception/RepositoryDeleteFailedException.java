package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class RepositoryDeleteFailedException extends InfrastructureException {

    public RepositoryDeleteFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.REPOSITORY_DELETE_FAILED, message, cause);
    }
}
