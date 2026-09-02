package io.jgitkins.server.identity.access.application.contract.result;

import java.time.LocalDateTime;
import java.util.List;
import io.jgitkins.server.identity.access.application.internal.UserIdentitySummary;

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
