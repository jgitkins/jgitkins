package io.jgitkins.web.application.dto;

public record UserCredentialIssueRequest(
		String name,
		String description,
		String expiration
) {
}
