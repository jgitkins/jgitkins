package io.jgitkins.server.common.presentation.error;

import io.jgitkins.core.common.error.ErrorCode;
import lombok.Getter;

@Getter
public enum PresentationErrorCode implements ErrorCode {
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized"),
    NOT_FOUND("NOT_FOUND", "Resource not found"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "Method not allowed"),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "Unsupported media type"),
    BAD_REQUEST("BAD_REQUEST", "Bad request");

    private final String code;
    private final String defaultMessage;

    PresentationErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

}
