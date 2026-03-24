package io.jgitkins.server.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCredentialIssueRequest(
        @NotBlank
        String name,

        @NotBlank
        String description,

        String expiration
) {
}
