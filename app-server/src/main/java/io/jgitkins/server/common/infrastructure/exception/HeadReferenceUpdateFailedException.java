package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class HeadReferenceUpdateFailedException extends InfrastructureException {

    public HeadReferenceUpdateFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.HEAD_REFERENCE_UPDATE_FAILED, message, cause);
    }
}
