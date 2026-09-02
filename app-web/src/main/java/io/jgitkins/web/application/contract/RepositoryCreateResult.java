package io.jgitkins.web.application.contract;

public record RepositoryCreateResult(
		RepositorySummary repository,
		String errorMessage
) {
}
