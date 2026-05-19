package io.jgitkins.server.identity.access.application.dto.result;

public record UserCredentialIssueResult(
        Long credentialId,
        String token
) {
}
