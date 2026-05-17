package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class HeadReferenceResolveFailedException extends InfrastructureException {

    public HeadReferenceResolveFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.HEAD_REFERENCE_RESOLVE_FAILED, message, cause);
    }
}
