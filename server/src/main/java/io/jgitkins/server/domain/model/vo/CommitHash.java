package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Git Commit Hash Value Object
 */
@Getter
@EqualsAndHashCode
public class CommitHash {
    private final String value;

    private CommitHash(String value) {
        if (value == null || value.length() < 7) {
            throw new IllegalArgumentException("Invalid commit hash: must be at least 7 characters");
        }
        this.value = value;
    }

    public static CommitHash of(String value) {
        return new CommitHash(value);
    }

    /**
     * Short hash (첫 7자리)
     */
    public String getShortHash() {
        return value.length() > 7 ? value.substring(0, 7) : value;
    }

    @Override
    public String toString() {
        return value;
    }
}
