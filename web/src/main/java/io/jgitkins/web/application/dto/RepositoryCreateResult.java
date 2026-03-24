package io.jgitkins.web.application.dto;

public record RepositoryCreateResult(
		RepositorySummary repository,
		String errorMessage
) {
}
