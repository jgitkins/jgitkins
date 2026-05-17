package io.jgitkins.server.change.review.domain.model.changegraph;

public enum MergeabilityStatus {
    MERGEABLE,
    CONFLICTING,
    NO_COMMON_ANCESTOR,
    UNKNOWN
}
