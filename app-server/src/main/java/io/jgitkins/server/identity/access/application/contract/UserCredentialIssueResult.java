package io.jgitkins.server.identity.access.application.contract;

public record UserCredentialIssueResult(
        Long credentialId,
        String token
) {
}
