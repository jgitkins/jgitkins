package io.jgitkins.web.application.contract;

public record UserCredentialIssueResult(
		Long credentialId,
		String token
) {
}
