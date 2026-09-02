package io.jgitkins.web.application.contract;

public record UserCredentialIssueRequest(
		String name,
		String description,
		String expiration
) {
}
