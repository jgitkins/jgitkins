package io.jgitkins.server.identity.access.application.contract;

import java.time.LocalDateTime;

public record UserSummary(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        LocalDateTime createdAt
) {
}
