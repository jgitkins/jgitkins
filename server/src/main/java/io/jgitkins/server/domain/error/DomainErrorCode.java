package io.jgitkins.server.domain.error;

import io.jgitkins.server.common.error.ErrorCode;

public enum DomainErrorCode implements ErrorCode {
    RULE_VIOLATION("RULE_VIOLATION", "Domain rule violation"),
    ORGANIZE_MEMBER_INVALID("ORGANIZE_MEMBER_INVALID", "Organize member payload is invalid"),
    ORGANIZE_MEMBER_ALREADY_EXISTS("ORGANIZE_MEMBER_ALREADY_EXISTS", "Organize member already exists"),
    RUNNER_ALREADY_ACTIVED("RUNNER_ALREADY_ACTIVED", "Runner already active"),
    RUNNER_TOKEN_INVALID("RUNNER_TOKEN_INVALID", "Runner token is invalid"),
    USER_ALREADY_ACTIVATED("USERNAME_ALREADY_SET", "Username already set");

    private final String code;
    private final String defaultMessage;

    DomainErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
