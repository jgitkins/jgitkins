package io.jgitkins.server.presentation.dto;

import java.time.LocalDateTime;

public record RepositoryUpdateRequest(
        String name,
        String path,
        String defaultBranch,
        String visibility,
        Long ownerId,
        String description,
        String credentialId,
        LocalDateTime lastSyncedAt
) {
}
