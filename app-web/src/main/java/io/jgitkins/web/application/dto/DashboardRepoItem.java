package io.jgitkins.web.application.dto;

import java.util.List;

public record DashboardRepoItem(
		String namespace,
		String ownerSlug,
		String repoName,
		RepositorySummary repository,
		List<CommitSummary> commits,
		int prCount,
		int issueCount,
		String prLink,
		String issueLink) {
}
