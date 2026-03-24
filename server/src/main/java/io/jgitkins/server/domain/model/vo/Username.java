package io.jgitkins.server.domain.model.vo;

import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Username value object.
 */
@Getter
@EqualsAndHashCode
public class Username {

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

    public boolean isOrganizeNameCompatible() {
        return OrganizeName.isValid(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
