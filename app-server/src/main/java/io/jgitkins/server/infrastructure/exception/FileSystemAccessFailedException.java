package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class FileSystemAccessFailedException extends InfrastructureException {

    public FileSystemAccessFailedException(String message) {
        super(InfrastructureProblemSpec.FILESYSTEM_ACCESS_FAILED, message);
    }

    public FileSystemAccessFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.FILESYSTEM_ACCESS_FAILED, message, cause);
    }
}
