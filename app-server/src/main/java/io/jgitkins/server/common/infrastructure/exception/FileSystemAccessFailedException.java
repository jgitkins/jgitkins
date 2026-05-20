package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class FileSystemAccessFailedException extends InfrastructureException {

    public FileSystemAccessFailedException(String message) {
        super(InfrastructureProblemSpec.FILESYSTEM_ACCESS_FAILED, message);
    }

    public FileSystemAccessFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.FILESYSTEM_ACCESS_FAILED, message, cause);
    }
}
