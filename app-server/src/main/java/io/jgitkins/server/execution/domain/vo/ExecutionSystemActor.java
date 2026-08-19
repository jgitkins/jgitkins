package io.jgitkins.server.execution.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class ExecutionSystemActor {
    public static final ExecutionSystemActor SYSTEM = new ExecutionSystemActor("SYSTEM");

    private final String value;

    private ExecutionSystemActor(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ExecutionSystemActor value cannot be null or empty");
        }
        this.value = value;
    }

    public static ExecutionSystemActor of(String value) {
        return new ExecutionSystemActor(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
