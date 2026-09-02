package io.jgitkins.server.identity.access.adapter.in.rest.contract.request;

import jakarta.validation.constraints.NotBlank;

public record UserCredentialIssueRequest(
        @NotBlank
        String name,

        @NotBlank
        String description,

        String expiration
) {
}
