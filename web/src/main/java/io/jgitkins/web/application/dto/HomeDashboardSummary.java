package io.jgitkins.web.application.dto;

import java.time.LocalDateTime;

public record HomeDashboardSummary(
        DashboardSummary dashboard,
        String displayName,
        boolean pendingUsername,
        String usernameSetupError,
        LocalDateTime lastUpdated) {
}
