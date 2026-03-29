package io.jgitkins.server.application.dto.result;

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
