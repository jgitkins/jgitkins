package io.jgitkins.web.application.contract;

import java.util.List;

public record DashboardSummary(
		List<OrganizeSummary> organizes,
		List<DashboardRepoItem> repositories,
		List<DashboardFeedItem> feed,
		String errorMessage) {
}
