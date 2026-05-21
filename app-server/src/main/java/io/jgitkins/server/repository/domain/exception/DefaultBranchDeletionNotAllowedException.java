package io.jgitkins.server.repository.domain.exception;

import io.jgitkins.server.shared.domain.exception.DomainException;
import io.jgitkins.server.shared.domain.error.DomainProblemSpec;

public class DefaultBranchDeletionNotAllowedException extends DomainException {

    public DefaultBranchDeletionNotAllowedException(String branchName) {
        super(DomainProblemSpec.DEFAULT_BRANCH_DELETION_NOT_ALLOWED,
                "Default branch cannot be deleted: " + branchName);
    }
}
