package io.jgitkins.runner.presentation.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jgitkins.runner.application.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiError {

    private final String code;
    private final String message;

    private ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiError of(ErrorCode errorCode) {
        return new ApiError(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public static ApiError of(ErrorCode errorCode, String message) {
        return new ApiError(errorCode.getCode(), message);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
