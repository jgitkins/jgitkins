package io.jgitkins.web.application.contract;

import java.time.LocalDateTime;

public record CommitSummary(
		String id,
		String authorName,
		String authorEmail,
		String shortMessage,
		LocalDateTime commitTime
) {
}
