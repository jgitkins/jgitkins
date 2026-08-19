package io.jgitkins.server.change.review.domain.model.vo;

import java.util.Objects;

public final class ReviewRepositoryId {
    private final Long value;
    private ReviewRepositoryId(Long value) {
        if (value == null || value <= 0) throw new IllegalArgumentException("ReviewRepositoryId must be a positive value");
        this.value = value;
    }
    public static ReviewRepositoryId of(Long value) { return new ReviewRepositoryId(value); }
    public Long getValue() { return value; }
    public Long value() { return value; }
    @Override public boolean equals(Object other) { return this == other || other instanceof ReviewRepositoryId that && Objects.equals(value, that.value); }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return String.valueOf(value); }
}
