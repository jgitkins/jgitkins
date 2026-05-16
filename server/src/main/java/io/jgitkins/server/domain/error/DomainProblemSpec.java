package io.jgitkins.server.domain.error;

import io.jgitkins.core.common.problem.ProblemSpec;

public enum DomainProblemSpec implements ProblemSpec<DomainErrorCode> {
    ORGANIZE_MEMBER_INVALID(DomainErrorCode.RULE_VIOLATION, "ORG-MEMBER-400", "Organize member payload is invalid", "organizeMember.invalid"),
    DEFAULT_BRANCH_DELETION_NOT_ALLOWED(DomainErrorCode.RULE_VIOLATION, "BRANCH-400-DEFAULT", "Default branch cannot be deleted", "branch.defaultDeletionNotAllowed"),
    RUNNER_ALREADY_ACTIVE(DomainErrorCode.INVALID_STATE, "RUNNER-409-ACTIVE", "Runner is already active", "runner.alreadyActive"),
    RUNNER_TOKEN_MISMATCH(DomainErrorCode.POLICY_VIOLATION, "RUNNER-422-TOKEN-MISMATCH", "Runner token does not match activation request", "runner.tokenMismatch"),
    RUNNER_TOKEN_MISSING(DomainErrorCode.POLICY_VIOLATION, "RUNNER-422-TOKEN-MISSING", "Runner activation token is required", "runner.tokenMissing"),
    USER_ALREADY_ACTIVATED(DomainErrorCode.INVALID_STATE, "USER-409-ACTIVATED", "User is already activated", "user.alreadyActivated");

    private final DomainErrorCode errorCode;
    private final String code;
    private final String defaultMessage;
    private final String messageKey;

    DomainProblemSpec(DomainErrorCode errorCode, String code, String defaultMessage, String messageKey) {
        this.errorCode = errorCode;
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.messageKey = messageKey;
    }

    @Override
    public DomainErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
