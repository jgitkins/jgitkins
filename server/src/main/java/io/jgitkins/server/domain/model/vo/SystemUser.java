package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 시스템 사용자 Value Object
 * JobHistory의 created_by 필드에 사용
 */
@Getter
@EqualsAndHashCode
public class SystemUser {
    public static final SystemUser SYSTEM = new SystemUser("SYSTEM");

    private final String value;

    private SystemUser(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SystemUser value cannot be null or empty");
        }
        this.value = value;
    }

    public static SystemUser of(String value) {
        return new SystemUser(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
