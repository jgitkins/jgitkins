package io.jgitkins.server.application.dto.result;

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
