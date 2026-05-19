package io.jgitkins.server.identity.access.application.dto.command;

public record UserCredentialIssueCommand(
        String name,
        String description,
        String expiration
) {
}
