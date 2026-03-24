package io.jgitkins.web.application.dto;

import java.util.List;

public record RepositoryCommits(
		String namespace,
		String repoName,
		RepositorySummary repository,
		List<CommitSummary> commits
) {
}
