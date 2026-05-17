package io.jgitkins.web.application.dto;

public record RepositoryBranchCreateResult(
		BranchSummary branch,
		String errorMessage
) {
}
