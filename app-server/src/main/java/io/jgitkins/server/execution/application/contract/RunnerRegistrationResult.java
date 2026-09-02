package io.jgitkins.server.execution.application.contract;

import java.time.LocalDateTime;

public record RunnerRegistrationResult(
        Long runnerId,
        String token,
        String status,
        LocalDateTime registeredAt
) {
}
