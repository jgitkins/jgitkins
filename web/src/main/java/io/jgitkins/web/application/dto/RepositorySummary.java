package io.jgitkins.web.application.dto;

import java.time.LocalDateTime;

public record RepositorySummary(
		Long id,
		String ownerType,
		String name,
		String path,
		String defaultBranch,
		String visibility,
		String description,
		Long ownerId,
		String clonePath,
		String cloneUrl,
		LocalDateTime lastSyncedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
