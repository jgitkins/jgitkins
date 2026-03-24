package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Repository URL Path(slug) Value Object
 */
@Getter
@EqualsAndHashCode
public class RepositoryPath {
    private static final Pattern ALLOWED = Pattern.compile("^[a-z0-9]([a-z0-9-_]*[a-z0-9])?$");

    private final String value;

    private RepositoryPath(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Repository path must not be null");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Repository path must not be blank");
        }
        if (!ALLOWED.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Repository path must match ^[a-z0-9]([a-z0-9-_]*[a-z0-9])?$");
        }
        this.value = normalized;
    }

    public static RepositoryPath from(String value) {
        return new RepositoryPath(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
