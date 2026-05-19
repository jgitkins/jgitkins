package io.jgitkins.server.identity.access.application.dto.result;

import java.time.LocalDateTime;

public record UserAdminSummary(
        Long id,
        String username,
        String email,
        String displayName,
        String status,
        LocalDateTime lastLoginAt
) {
}
