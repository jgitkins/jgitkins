package io.jgitkins.web.application.dto;

import java.time.LocalDateTime;

public record OrganizeSummary(
		Long id,
		String name,
		String description,
		Long ownerId,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
