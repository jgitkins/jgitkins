package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class BranchNotFoundException extends ApplicationException {

    public BranchNotFoundException(String branchName) {
        super(ApplicationProblemSpec.BRANCH_NOT_FOUND, "Branch not found: " + branchName);
    }

    public BranchNotFoundException(String message, boolean rawMessage) {
        super(ApplicationProblemSpec.BRANCH_NOT_FOUND, message);
    }
}
