package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class RepositoryCreateFailedException extends InfrastructureException {

    public RepositoryCreateFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.REPOSITORY_CREATE_FAILED, message, cause);
    }
}
