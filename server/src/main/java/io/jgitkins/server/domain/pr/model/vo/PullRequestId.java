package io.jgitkins.server.domain.pr.model.vo;

public record PullRequestId(Long value) {

    public PullRequestId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("PullRequestId must be a positive value");
        }
    }

    public static PullRequestId of(Long value) {
        return new PullRequestId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
