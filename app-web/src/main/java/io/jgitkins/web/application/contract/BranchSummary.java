package io.jgitkins.web.application.contract;

public record BranchSummary(
		Long repositoryId,
		String name,
		boolean locked,
		boolean ciEnabled,
		boolean defaultBranch
) {
}
