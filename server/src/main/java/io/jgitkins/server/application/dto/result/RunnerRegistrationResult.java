package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;

public record RunnerRegistrationResult(
        Long runnerId,
        String token,
        String status,
        LocalDateTime registeredAt
) {
}
