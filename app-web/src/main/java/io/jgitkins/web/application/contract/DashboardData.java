package io.jgitkins.web.application.contract;

import java.util.List;

public record DashboardData(
		List<OrganizeSummary> organizes,
		List<RepositoryCommits> repositories,
		String errorMessage
) {
}
