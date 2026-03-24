package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * Job 식별자 Value Object
 */
@Getter
@EqualsAndHashCode
public class JobId {
    private final String value;

    private JobId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JobId cannot be null or empty");
        }
        this.value = value;
    }

    public static JobId of(String value) {
        return new JobId(value);
    }

    public static JobId generate() {
        return new JobId("JOB_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
