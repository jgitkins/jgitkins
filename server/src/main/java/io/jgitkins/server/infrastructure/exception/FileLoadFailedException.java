package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class FileLoadFailedException extends InfrastructureException {

    public FileLoadFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.FILE_LOAD_FAILED, message, cause);
    }
}
