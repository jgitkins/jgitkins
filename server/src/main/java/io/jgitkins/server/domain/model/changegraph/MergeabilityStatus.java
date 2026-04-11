package io.jgitkins.server.domain.model.changegraph;

public enum MergeabilityStatus {
    MERGEABLE,
    CONFLICTING,
    NO_COMMON_ANCESTOR,
    UNKNOWN
}
