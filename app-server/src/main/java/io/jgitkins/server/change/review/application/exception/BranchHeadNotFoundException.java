package io.jgitkins.server.change.review.application.exception;

import io.jgitkins.server.shared.application.error.ApplicationProblemSpec;
import io.jgitkins.server.shared.application.exception.ApplicationException;

public final class BranchHeadNotFoundException extends ApplicationException {
    public BranchHeadNotFoundException(String branchName) { super(ApplicationProblemSpec.BRANCH_NOT_FOUND, "Branch not found: " + branchName); }
}
