package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class FileReadFailedException extends InfrastructureException {

    public FileReadFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.FILE_READ_FAILED, message, cause);
    }
}
