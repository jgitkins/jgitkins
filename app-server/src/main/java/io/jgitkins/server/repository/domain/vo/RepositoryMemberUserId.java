package io.jgitkins.server.repository.domain.vo;

import java.util.Objects;

public final class RepositoryMemberUserId {
    private final Long value;

    private RepositoryMemberUserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("RepositoryMemberUserId must be a positive value");
        }
        this.value = value;
    }

    public static RepositoryMemberUserId of(Long value) {
        return new RepositoryMemberUserId(value);
    }

    public Long value() {
        return value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepositoryMemberUserId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
