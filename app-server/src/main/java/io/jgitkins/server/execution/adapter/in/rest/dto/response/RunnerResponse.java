package io.jgitkins.server.execution.adapter.in.rest.dto.response;

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
