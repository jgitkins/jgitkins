package io.jgitkins.server.collaboration.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Collaboration context's member user identifier.
 */
@Getter
@EqualsAndHashCode
public final class MemberUserId {

    private final Long value;

    private MemberUserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("MemberUserId must be a positive value");
        }
        this.value = value;
    }

    public static MemberUserId of(Long value) {
        return new MemberUserId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
