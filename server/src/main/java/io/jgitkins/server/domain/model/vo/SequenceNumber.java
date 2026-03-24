package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * JobHistory 순서 번호 Value Object
 */
@Getter
@EqualsAndHashCode
public class SequenceNumber {
    private final int value;

    private SequenceNumber(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Sequence number must be positive");
        }
        this.value = value;
    }

    public static SequenceNumber of(int value) {
        return new SequenceNumber(value);
    }

    public static SequenceNumber first() {
        return new SequenceNumber(1);
    }

    public SequenceNumber next() {
        return new SequenceNumber(value + 1);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
