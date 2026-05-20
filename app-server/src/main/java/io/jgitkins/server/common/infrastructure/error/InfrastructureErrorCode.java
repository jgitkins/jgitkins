package io.jgitkins.server.common.infrastructure.error;

import io.jgitkins.core.common.error.ErrorCode;

public enum InfrastructureErrorCode implements ErrorCode {
    INTERNAL_ERROR("INTERNAL_ERROR", "Infrastructure internal error"),
    PERSISTENCE_OPERATION_FAILED("PERSISTENCE_OPERATION_FAILED", "Persistence operation failed"),
    JGIT_OPERATION_FAILED("JGIT_OPERATION_FAILED", "JGit operation failed"),
    FILESYSTEM_ACCESS_FAILED("FILESYSTEM_ACCESS_FAILED", "Filesystem access failed");

    private final String code;
    private final String defaultMessage;

    InfrastructureErrorCode(String code, String defaultMessage) {
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
