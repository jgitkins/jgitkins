package io.jgitkins.server.collaboration.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Collaboration context's owner identifier.
 *
 * <p>This is intentionally distinct from identity.access.domain.vo.UserId.
 */
@Getter
@EqualsAndHashCode
public final class OwnerId {

    private final Long value;

    private OwnerId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("OwnerId must be a positive value");
        }
        this.value = value;
    }

    public static OwnerId of(Long value) {
        return new OwnerId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
