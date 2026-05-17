package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class HeadReferenceUpdateFailedException extends InfrastructureException {

    // TODO: 인프라 예외가 너무 상세한거같은데.. 유지해도될지 말지 검토 필요
    public HeadReferenceUpdateFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.HEAD_REFERENCE_UPDATE_FAILED, message, cause);
    }
}
