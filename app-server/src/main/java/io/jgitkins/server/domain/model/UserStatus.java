package io.jgitkins.server.domain.model;

import java.util.Locale;

public enum UserStatus {
    ACTIVE,
    PENDING,
    BLOCKED,
    DELETED;

    public static UserStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("PENDING_USERNAME".equals(normalized)) {
            return PENDING;
        }
        return UserStatus.valueOf(normalized);
    }

    public static UserStatus fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return fromString(value);
    }
}
