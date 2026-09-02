package io.jgitkins.server.collaboration.application.contract;

import java.time.LocalDateTime;

public record OrganizeCreationResult(
        Long id,
        String name,
        String description,
        Long ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
