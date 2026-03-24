package io.jgitkins.server.presentation.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jgitkins.server.common.error.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiError {

    private final String code;
    private final String message;
    private final String source;

    private ApiError(String code, String message, String source) {
        this.code = code;
        this.message = message;
        this.source = source;
    }

    public static ApiError of(ErrorCode errorCode) {
        return new ApiError(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    public static ApiError of(ErrorCode errorCode, String message) {
        return new ApiError(errorCode.getCode(), message, null);
    }

    public static ApiError of(ErrorCode errorCode, String message, String source) {
        return new ApiError(errorCode.getCode(), message, source);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }
}
