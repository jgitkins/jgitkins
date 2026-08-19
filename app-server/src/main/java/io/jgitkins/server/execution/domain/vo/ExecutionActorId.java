package io.jgitkins.server.execution.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class ExecutionActorId {
    private final Long value;

    private ExecutionActorId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ExecutionActorId must be a positive value");
        }
        this.value = value;
    }

    public static ExecutionActorId of(Long value) {
        return new ExecutionActorId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
