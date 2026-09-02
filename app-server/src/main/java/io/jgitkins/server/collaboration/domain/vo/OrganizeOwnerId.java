package io.jgitkins.server.collaboration.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Collaboration context's owner identifier.
 *
 * <p>This is intentionally distinct from identity.access.domain.vo.UserId.
 */
import io.jgitkins.server.shared.domain.exception.InvalidIdentifierException;

@Getter
@EqualsAndHashCode
public final class OrganizeOwnerId {

    private final Long value;

    private OrganizeOwnerId(Long value) {
        if (value == null || value <= 0) {
            throw new InvalidIdentifierException("OrganizeOwnerId must be a positive value");
        }
        this.value = value;
    }

    public static OrganizeOwnerId of(Long value) {
        return new OrganizeOwnerId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
