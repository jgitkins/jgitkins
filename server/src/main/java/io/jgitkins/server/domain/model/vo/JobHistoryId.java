package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * JobHistory 식별자 Value Object
 */
@Getter
@EqualsAndHashCode
public class JobHistoryId {
    private final String value;

    private JobHistoryId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JobHistoryId cannot be null or empty");
        }
        this.value = value;
    }

    public static JobHistoryId of(String value) {
        return new JobHistoryId(value);
    }

    public static JobHistoryId generate() {
        return new JobHistoryId("HIST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
