package io.jgitkins.server.identity.access.domain.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/** Username value object. */
public final class Username {
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9._-]+$");
    private final String value;

    private Username(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Username must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (!ALLOWED.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Username allows only letters, numbers, dot, hyphen, or underscore");
        }
        this.value = trimmed;
    }

    public static Username from(String value) {
        return new Username(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Username username)) return false;
        return value.equals(username.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
