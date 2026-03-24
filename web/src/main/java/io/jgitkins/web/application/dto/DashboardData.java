package io.jgitkins.web.application.dto;

import java.util.List;

public record DashboardData(
		List<OrganizeSummary> organizes,
		List<RepositoryCommits> repositories,
		String errorMessage
) {
}
