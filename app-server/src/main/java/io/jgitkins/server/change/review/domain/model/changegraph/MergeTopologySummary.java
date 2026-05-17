package io.jgitkins.server.change.review.domain.model.changegraph;

public record MergeTopologySummary(
        Boolean fastForwardPossible,
        Boolean mergeCommitRequired
) {

    public static MergeTopologySummary known(boolean fastForwardPossible, boolean mergeCommitRequired) {
        return new MergeTopologySummary(fastForwardPossible, mergeCommitRequired);
    }

    public static MergeTopologySummary unknown() {
        return new MergeTopologySummary(null, null);
    }
}
