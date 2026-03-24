package io.jgitkins.web.application.dto;

import java.time.LocalDateTime;

public record UserSummary(
		Long id,
		String username,
		String displayName,
		String avatarUrl,
		LocalDateTime createdAt
) {
}
