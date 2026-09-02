package io.jgitkins.server.execution.adapter.in.rest.contract.response;

import java.time.LocalDateTime;

public record RunnerResponse(
        Long runnerId,
        String description,
        String status,
        LocalDateTime lastHeartbeatAt,
        LocalDateTime registeredAt
) {
}
