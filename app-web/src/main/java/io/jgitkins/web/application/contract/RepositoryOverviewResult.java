package io.jgitkins.web.application.contract;

import java.util.List;

public record RepositoryOverviewResult(
		RepositorySummary repository,
		List<BranchSummary> branches,
		List<RepositoryFileEntry> tree,
		String selectedBranch,
		String role,
		boolean writable
) {
}
