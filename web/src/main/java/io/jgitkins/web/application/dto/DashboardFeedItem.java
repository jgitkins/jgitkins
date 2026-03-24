package io.jgitkins.web.application.dto;

import java.time.LocalDateTime;

public record DashboardFeedItem(
		String namespace,
		String repoName,
		String shortMessage,
		String authorName,
		LocalDateTime commitTime) {
}
