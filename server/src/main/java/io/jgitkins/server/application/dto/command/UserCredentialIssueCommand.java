package io.jgitkins.server.application.dto.command;

public record UserCredentialIssueCommand(
        String name,
        String description,
        String expiration
) {
}
