package io.jgitkins.core.web.api.response;

import io.jgitkins.core.common.error.ErrorCode;
import io.jgitkins.core.common.problem.ProblemSpec;
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

    private static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null);
    }

    private static ApiResponse<Void> success() {
        return new ApiResponse<>(null, null);
    }

    private static ApiResponse<Void> failure(ErrorCode errorCode, String message, String source) {
        return new ApiResponse<>(null, ApiError.of(errorCode, message, source));
    }

    private static ApiResponse<Void> failure(ProblemSpec<? extends ErrorCode> problemSpec, String message, String source) {
        return new ApiResponse<>(null, ApiError.of(problemSpec, message, source));
    }

    private static ApiResponse<Void> failure(String code, String message, String source) {
        return new ApiResponse<>(null, ApiError.of(code, message, source));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(Object resourceId, T body) {
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resourceId)
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.success(body));
    }

    public static ResponseEntity<ApiResponse<Void>> created(URI location) {
        return ResponseEntity.created(location).body(ApiResponse.success());
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(URI location, T body) {
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

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status,
                                                          ErrorCode errorCode,
                                                          String message,
                                                          String source) {
        return ResponseEntity.status(status).body(failure(errorCode, message, source));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status,
                                                          ProblemSpec<? extends ErrorCode> problemSpec,
                                                          String message,
                                                          String source) {
        return ResponseEntity.status(status).body(failure(problemSpec, message, source));
    }

    public static ResponseEntity<ApiResponse<Void>> error(HttpStatus status,
                                                          String code,
                                                          String message,
                                                          String source) {
        return ResponseEntity.status(status).body(failure(code, message, source));
    }

    public static ApiResponse<Void> errorBody(ProblemSpec<? extends ErrorCode> problemSpec,
                                              String message,
                                              String source) {
        return failure(problemSpec, message, source);
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
