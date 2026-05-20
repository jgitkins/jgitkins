package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class RepositoryDeleteFailedException extends InfrastructureException {

    public RepositoryDeleteFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.REPOSITORY_DELETE_FAILED, message, cause);
    }
}
