package io.jgitkins.web.application.dto;

import java.util.List;

public record RepositoryDetailData(
		RepositorySummary repository,
		List<BranchSummary> branches,
		List<RepositoryFileEntry> files,
		String namespace,
		String ownerSlug,
		String repoName,
		String selectedBranch,
		String role,
		boolean writable,
		String errorMessage
) {
}
