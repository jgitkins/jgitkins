package io.jgitkins.web.presentation.common;

public record ApiResponse<T>(T data, ApiError error) {
}
