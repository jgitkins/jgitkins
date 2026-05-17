package io.jgitkins.web.application.dto;

import java.util.List;

public record DashboardSummary(
		List<OrganizeSummary> organizes,
		List<DashboardRepoItem> repositories,
		List<DashboardFeedItem> feed,
		String errorMessage) {
}
