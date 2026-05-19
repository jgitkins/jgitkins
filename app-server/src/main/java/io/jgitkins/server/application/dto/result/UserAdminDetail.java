package io.jgitkins.server.identity.access.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record UserAdminDetail(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl,
        String status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<UserIdentitySummary> identities
) {
}
