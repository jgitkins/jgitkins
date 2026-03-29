package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;

public record RepositoryResult(
        Long id,
        String ownerType,
        String name,
        String path,
        String defaultBranch,
        String visibility,
        String description,
        Long ownerId,
        String credentialId,
        String clonePath,
        String cloneUrl,
        boolean requiresInitialContent,
        LocalDateTime lastSyncedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
