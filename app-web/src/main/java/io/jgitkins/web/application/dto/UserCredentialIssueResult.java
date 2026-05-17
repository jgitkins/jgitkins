package io.jgitkins.web.application.dto;

public record UserCredentialIssueResult(
		Long credentialId,
		String token
) {
}
