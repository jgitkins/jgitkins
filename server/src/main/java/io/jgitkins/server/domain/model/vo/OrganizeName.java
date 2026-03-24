package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Organize 표시 이름 VO
 */
@Getter
@EqualsAndHashCode
public class OrganizeName {

    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final String value;

    private OrganizeName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Organize name must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Organize name must not be blank");
        }
        if (!ALLOWED.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Organize name allows only alphanumeric characters, hyphen, or underscore");
        }
        this.value = trimmed;
    }

    public static OrganizeName from(String value) {
        return new OrganizeName(value);
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return ALLOWED.matcher(trimmed).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
