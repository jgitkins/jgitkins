package io.jgitkins.server.repository.application.exception;

import io.jgitkins.server.shared.application.exception.ApplicationException;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;

public class SourceBranchNotFoundException extends ApplicationException {

    public SourceBranchNotFoundException(String branchName) {
        super(ApplicationProblemSpec.SOURCE_BRANCH_NOT_FOUND, "Source branch not found: " + branchName);
    }

    public SourceBranchNotFoundException(String message, boolean rawMessage) {
        super(ApplicationProblemSpec.SOURCE_BRANCH_NOT_FOUND, message);
    }
}
