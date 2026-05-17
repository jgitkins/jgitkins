package io.jgitkins.runner.presentation.common;


import io.jgitkins.runner.application.exception.ErrorCode;

/**
 * Standardized wrapper that matches the README contract (`data` payload with optional `error`).
 */
public final class ApiResponse<T> {

    private final T data;
    private final ApiError error;

    private ApiResponse(T data, ApiError error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(null, ApiError.of(errorCode));
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(null, ApiError.of(errorCode, message));
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
