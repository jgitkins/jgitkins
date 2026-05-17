package io.jgitkins.server.change.review.domain.model.changegraph;

import java.util.List;

public record MergeabilityAssessment(
        MergeabilityStatus status,
        MergeTopologySummary topology,
        List<String> conflicts,
        String reason
) {

    public MergeabilityAssessment {
        status = status == null ? MergeabilityStatus.UNKNOWN : status;
        topology = topology == null ? MergeTopologySummary.unknown() : topology;
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }
}
