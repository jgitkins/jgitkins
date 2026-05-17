package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class OwnerId {
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
