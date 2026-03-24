package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Repository 식별자 Value Object
 */
@Getter
@EqualsAndHashCode
public class RepositoryId {
    private final Long value;

    private RepositoryId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("RepositoryId must be a positive value");
        }
        this.value = value;
    }

    public static RepositoryId of(Long value) {
        return new RepositoryId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
