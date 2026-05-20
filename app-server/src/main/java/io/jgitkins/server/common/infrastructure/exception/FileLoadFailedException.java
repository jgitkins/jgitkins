package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class FileLoadFailedException extends InfrastructureException {

    public FileLoadFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.FILE_LOAD_FAILED, message, cause);
    }
}
