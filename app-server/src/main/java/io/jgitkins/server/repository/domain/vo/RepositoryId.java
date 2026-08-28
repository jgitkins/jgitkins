package io.jgitkins.server.repository.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Repository 식별자 Value Object
 */
import io.jgitkins.server.shared.domain.exception.InvalidIdentifierException;

@Getter
@EqualsAndHashCode
public class RepositoryId {
    private final Long value;

    private RepositoryId(Long value) {
        if (value == null || value <= 0) {
            throw new InvalidIdentifierException("RepositoryId must be a positive value");
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
