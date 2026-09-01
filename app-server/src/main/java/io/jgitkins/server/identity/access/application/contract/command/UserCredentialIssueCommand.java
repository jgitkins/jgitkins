package io.jgitkins.server.identity.access.application.contract.command;

public record UserCredentialIssueCommand(
        String name,
        String description,
        String expiration
) {
}
