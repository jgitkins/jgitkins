package io.jgitkins.server.shared.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class RepositoryOwnerId {
    private final Long value;

    private RepositoryOwnerId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("RepositoryOwnerId must be a positive value");
        }
        this.value = value;
    }

    public static RepositoryOwnerId of(Long value) {
        return new RepositoryOwnerId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
