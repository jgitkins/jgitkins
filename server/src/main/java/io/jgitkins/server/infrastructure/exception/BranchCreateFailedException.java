package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class BranchCreateFailedException extends InfrastructureException {

    public BranchCreateFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.BRANCH_CREATE_FAILED, message, cause);
    }
}
