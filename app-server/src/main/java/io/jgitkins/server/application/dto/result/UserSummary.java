package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;

public record UserSummary(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        LocalDateTime createdAt
) {
}
