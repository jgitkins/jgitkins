package io.jgitkins.server.presentation.common.error;

import io.jgitkins.server.common.error.ErrorCode;

public enum PresentationErrorCode implements ErrorCode {
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized"),
    BAD_REQUEST("BAD_REQUEST", "Bad request");

    private final String code;
    private final String defaultMessage;

    PresentationErrorCode(String code, String defaultMessage) {
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
