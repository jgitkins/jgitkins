package io.jgitkins.server.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RunnerActivateRequest(
        @NotBlank
        String token
) {
}
