package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class BranchCreateFailedException extends InfrastructureException {

    public BranchCreateFailedException(String message) {
        super(InfrastructureProblemSpec.BRANCH_CREATE_FAILED, message);
    }

    public BranchCreateFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.BRANCH_CREATE_FAILED, message, cause);
    }
}
