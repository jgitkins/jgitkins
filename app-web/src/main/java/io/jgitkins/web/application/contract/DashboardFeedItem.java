package io.jgitkins.web.application.contract;

import java.time.LocalDateTime;

public record DashboardFeedItem(
		String namespace,
		String repoName,
		String shortMessage,
		String authorName,
		LocalDateTime commitTime) {
}
