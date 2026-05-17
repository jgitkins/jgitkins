package io.jgitkins.server.infrastructure.exception;

import io.jgitkins.server.infrastructure.common.error.InfrastructureProblemSpec;

public class BranchDeleteFailedException extends InfrastructureException {

    public BranchDeleteFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.BRANCH_DELETE_FAILED, message, cause);
    }
}
