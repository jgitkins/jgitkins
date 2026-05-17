package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Organize(Organization) 식별자 Value Object
 */
@Getter
@EqualsAndHashCode
public class OrganizeId {
    private final Long value;

    private OrganizeId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("OrganizeId must be a positive value");
        }
        this.value = value;
    }

    public static OrganizeId of(Long value) {
        return new OrganizeId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
