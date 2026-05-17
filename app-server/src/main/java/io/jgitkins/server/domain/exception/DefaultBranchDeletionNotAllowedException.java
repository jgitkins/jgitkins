package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainProblemSpec;

public class DefaultBranchDeletionNotAllowedException extends DomainException {

    public DefaultBranchDeletionNotAllowedException(String branchName) {
        super(DomainProblemSpec.DEFAULT_BRANCH_DELETION_NOT_ALLOWED,
                "Default branch cannot be deleted: " + branchName);
    }
}
