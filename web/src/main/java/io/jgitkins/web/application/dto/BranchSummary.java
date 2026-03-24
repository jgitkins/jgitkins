package io.jgitkins.web.application.dto;

public record BranchSummary(
		Long repositoryId,
		String name,
		boolean locked,
		boolean ciEnabled,
		boolean defaultBranch
) {
}
