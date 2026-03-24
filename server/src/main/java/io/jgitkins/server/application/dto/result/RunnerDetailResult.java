package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerDetailResult {
    private final Long runnerId;
    private final String token;
    private final String description;
    private final String status;
    private final LocalDateTime lastHeartbeatAt;
    private final LocalDateTime registeredAt;
}
