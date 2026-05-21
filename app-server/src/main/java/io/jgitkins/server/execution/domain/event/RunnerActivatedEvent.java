package io.jgitkins.server.execution.domain.event;

import io.jgitkins.server.shared.domain.event.DomainEvent;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import io.jgitkins.server.execution.domain.vo.RunnerStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RunnerActivatedEvent implements DomainEvent {

    private final Long runnerId;
    private final RunnerStatus status;
    private final RunnerScopeType scopeType;
    private final Long scopeTargetId;
    private final String ipAddress;
    private final Instant occurredAt;

    public static RunnerActivatedEvent from(Runner runner) {
        return new RunnerActivatedEvent(
                runner.getId(),
                runner.getStatus(),
                runner.getScopeType(),
                runner.getScopeTargetId(),
                runner.getIpAddress(),
                Instant.now()
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
