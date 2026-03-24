package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Runner 식별자 Value Object
 */
@Getter
@EqualsAndHashCode
public class RunnerId {
    private final String value;

    private RunnerId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RunnerId cannot be null or empty");
        }
        this.value = value;
    }

    public static RunnerId of(String value) {
        return new RunnerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
