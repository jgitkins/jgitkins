package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class BranchAlreadyExistsException extends ApplicationException {

    public BranchAlreadyExistsException(String branchName) {
        super(ApplicationProblemSpec.BRANCH_ALREADY_EXISTS, "Branch already exists: " + branchName);
    }

    public BranchAlreadyExistsException(String message, boolean rawMessage) {
        super(ApplicationProblemSpec.BRANCH_ALREADY_EXISTS, message);
    }
}
