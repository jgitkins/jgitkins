package io.jgitkins.web.application.contract;

import java.time.LocalDateTime;

public record UserCredentialSummary(
		Long id,
		String provider,
		String name,
		String description,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
