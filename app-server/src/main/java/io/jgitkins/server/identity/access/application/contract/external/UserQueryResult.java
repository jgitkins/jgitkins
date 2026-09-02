package io.jgitkins.server.identity.access.application.contract.external;

import java.time.LocalDateTime;

public record UserQueryResult(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
