package io.jgitkins.server.domain.model.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Git Branch Name Value Object
 */
@Getter
@EqualsAndHashCode
public class BranchName {
    private final String value;

    private BranchName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Branch name cannot be null or empty");
        }
        this.value = value;
    }

    public static BranchName of(String value) {
        return new BranchName(value);
    }

    /**
     * 기본 브랜치인지 확인 (main, master)
     */
    public boolean isDefaultBranch() {
        return "main".equals(value) || "master".equals(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
