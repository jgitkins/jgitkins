package io.jgitkins.server.execution.adapter.in.rest.contract.request;

import jakarta.validation.constraints.NotBlank;

public record RunnerActivateRequest(
        @NotBlank
        String token
) {
}
