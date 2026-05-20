package io.jgitkins.server.common.infrastructure.exception;

import io.jgitkins.server.common.infrastructure.error.InfrastructureProblemSpec;

public class BranchDeleteFailedException extends InfrastructureException {

    public BranchDeleteFailedException(String message, Throwable cause) {
        super(InfrastructureProblemSpec.BRANCH_DELETE_FAILED, message, cause);
    }
}
