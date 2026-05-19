package io.jgitkins.server.collaboration.application.dto.result;

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
