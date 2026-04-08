package io.jgitkins.server.application.common.error;

import io.jgitkins.server.common.error.ErrorCode;

public enum ApplicationErrorCode implements ErrorCode {
    UNAUTHENTICATED("UNAUTHENTICATED", "Authentication required"),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied"),
    NOT_FOUND("NOT_FOUND", "Requested resource not found"),
    ALREADY_EXISTS("ALREADY_EXISTS", "Resource already exists"),
    UNPROCESSABLE("UNPROCESSABLE", "Request could not be processed");

    private final String code;
    private final String defaultMessage;

    ApplicationErrorCode(String code, String defaultMessage) {
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
