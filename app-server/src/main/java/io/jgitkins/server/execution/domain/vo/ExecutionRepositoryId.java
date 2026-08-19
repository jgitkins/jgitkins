package io.jgitkins.server.execution.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class ExecutionRepositoryId {
    private final Long value;

    private ExecutionRepositoryId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ExecutionRepositoryId must be a positive value");
        }
        this.value = value;
    }

    public static ExecutionRepositoryId of(Long value) {
        return new ExecutionRepositoryId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
