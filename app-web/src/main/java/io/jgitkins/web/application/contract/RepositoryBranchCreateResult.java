package io.jgitkins.web.application.contract;

public record RepositoryBranchCreateResult(
		BranchSummary branch,
		String errorMessage
) {
}
