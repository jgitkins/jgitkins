package io.jgitkins.server.domain.exception;

import io.jgitkins.server.domain.error.DomainErrorCode;

public class DefaultBranchDeletionNotAllowedException extends DomainException {

    public DefaultBranchDeletionNotAllowedException(String branchName) {
        super(DomainErrorCode.RULE_VIOLATION, "Default branch cannot be deleted: " + branchName);
    }
}
