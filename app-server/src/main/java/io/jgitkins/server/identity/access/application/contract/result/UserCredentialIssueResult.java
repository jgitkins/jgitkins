package io.jgitkins.server.identity.access.application.contract.result;

public record UserCredentialIssueResult(
        Long credentialId,
        String token
) {
}
