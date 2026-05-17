package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class RepositoryCreateFailedException extends InfrastructureException {

    public RepositoryCreateFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.REPOSITORY_CREATE_FAILED, message, cause);
    }
}
