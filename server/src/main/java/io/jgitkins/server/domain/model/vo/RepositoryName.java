package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Repository 표시 이름 Value Object
 */
@Getter
@EqualsAndHashCode
public class RepositoryName {
    private final String value;

    private RepositoryName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Repository name must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Repository name must not be blank");
        }
        this.value = trimmed;
    }

    public static RepositoryName from(String value) {
        return new RepositoryName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
