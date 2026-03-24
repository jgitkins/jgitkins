package io.jgitkins.server.presentation.common;

import io.jgitkins.server.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, String source) {
        return new ApiResponse<>(null, ApiError.of(errorCode, message, source));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(Object resourceId, T body) {
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resourceId)
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(body));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T body) {
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    public static ResponseEntity<ApiResponse<Void>> ok() {
        return ResponseEntity.ok(ApiResponse.success());
    }

    public static ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success());
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
