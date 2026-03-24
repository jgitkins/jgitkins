package io.jgitkins.server.presentation.dto;

import java.time.LocalDateTime;

public record RunnerResponse(
        Long runnerId,
        String token,
        String description,
        String status,
        LocalDateTime lastHeartbeatAt,
        LocalDateTime registeredAt
) {
}
