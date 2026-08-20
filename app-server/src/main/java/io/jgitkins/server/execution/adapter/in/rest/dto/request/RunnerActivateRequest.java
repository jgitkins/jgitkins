package io.jgitkins.server.execution.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RunnerActivateRequest(
        @NotBlank
        String token
) {
}
