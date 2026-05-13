package io.jgitkins.server.execution.application.contract.result;

import java.time.LocalDateTime;

public record RunnerDetailResult(
        Long runnerId,
        String token,
        String description,
        String status,
        LocalDateTime lastHeartbeatAt,
        LocalDateTime registeredAt
) {
}
