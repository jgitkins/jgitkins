package io.jgitkins.server.domain.model.vo;

import java.util.Locale;

public enum RepositoryVisibility {
    PUBLIC,
    PRIVATE,
    INTERNAL;

    public static RepositoryVisibility from(String value) {
        if (value == null) {
            return PRIVATE;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (RepositoryVisibility visibility : values()) {
            if (visibility.name().equals(normalized)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unsupported repository visibility: " + value);
    }
}
