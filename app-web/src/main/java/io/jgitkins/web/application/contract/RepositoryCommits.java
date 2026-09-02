package io.jgitkins.web.application.contract;

import java.util.List;

public record RepositoryCommits(
		String namespace,
		String repoName,
		RepositorySummary repository,
		List<CommitSummary> commits
) {
}
