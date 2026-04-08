package io.jgitkins.server.domain.error;

import io.jgitkins.server.common.error.ErrorCode;

public enum DomainErrorCode implements ErrorCode {
    RULE_VIOLATION("RULE_VIOLATION", "Domain rule violation"),
    INVALID_STATE("INVALID_STATE", "Domain state transition is invalid"),
    POLICY_VIOLATION("POLICY_VIOLATION", "Domain policy violation");

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
