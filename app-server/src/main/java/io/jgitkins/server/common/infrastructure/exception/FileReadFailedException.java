package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class FileReadFailedException extends InfrastructureException {

    public FileReadFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.FILE_READ_FAILED, message, cause);
    }
}
