package io.jgitkins.server.execution.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RunnerActivateRequest(
        @NotBlank
        String token
) {
}
