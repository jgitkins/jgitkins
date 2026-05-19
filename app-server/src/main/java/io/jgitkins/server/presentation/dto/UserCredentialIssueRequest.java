package io.jgitkins.server.identity.access.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UserCredentialIssueRequest(
        @NotBlank
        String name,

        @NotBlank
        String description,

        String expiration
) {
}
