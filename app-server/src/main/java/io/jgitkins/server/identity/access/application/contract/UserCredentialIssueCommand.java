package io.jgitkins.server.identity.access.application.contract;

public record UserCredentialIssueCommand(
        String name,
        String description,
        String expiration
) {
}
