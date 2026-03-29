package io.jgitkins.server.application.dto.result;

public record UserCredentialIssueResult(
        Long credentialId,
        String token
) {
}
