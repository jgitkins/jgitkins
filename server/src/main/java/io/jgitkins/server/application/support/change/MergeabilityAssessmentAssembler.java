package io.jgitkins.server.application.support.change;

import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.model.changegraph.MergeabilityStatus;
import io.jgitkins.server.domain.model.changegraph.MergeTopologySummary;
import org.springframework.stereotype.Component;

@Component
public class MergeabilityAssessmentAssembler {

    public MergeabilityAssessment toAssessment(MergeResult result) {
        if (result == null || result.getStatus() == null) {
            return new MergeabilityAssessment(
                    MergeabilityStatus.UNKNOWN,
                    MergeTopologySummary.unknown(),
                    null,
                    "Mergeability has not been evaluated yet.");
        }

        MergeabilityStatus status = toStatus(result.getStatus());
        MergeTopologySummary topology = toTopology(result);

        return new MergeabilityAssessment(
                status,
                topology,
                result.getConflicts(),
                reasonFor(status, topology));
    }

    private MergeabilityStatus toStatus(MergeResult.Status status) {
        return switch (status) {
            case MERGEABLE, MERGED, ALREADY_UP_TO_DATE -> MergeabilityStatus.MERGEABLE;
            case CONFLICTS -> MergeabilityStatus.CONFLICTING;
            case NO_COMMON_ANCESTOR -> MergeabilityStatus.NO_COMMON_ANCESTOR;
        };
    }

    private MergeTopologySummary toTopology(MergeResult result) {
        if (result.getFastForwardPossible() == null || result.getMergeCommitRequired() == null) {
            return MergeTopologySummary.unknown();
        }
        return MergeTopologySummary.known(result.getFastForwardPossible(), result.getMergeCommitRequired());
    }

    private String reasonFor(MergeabilityStatus status, MergeTopologySummary topology) {
        return switch (status) {
            case MERGEABLE -> mergeableReason(topology);
            case CONFLICTING -> "The source branch currently conflicts with the target branch.";
            case NO_COMMON_ANCESTOR -> "The source and target branches do not share a common ancestor.";
            case UNKNOWN -> "Mergeability has not been evaluated yet.";
        };
    }

    private String mergeableReason(MergeTopologySummary topology) {
        if (Boolean.TRUE.equals(topology.fastForwardPossible())) {
            return "The source branch is ahead of the target, so fast-forward is topologically possible.";
        }
        if (Boolean.TRUE.equals(topology.mergeCommitRequired())) {
            return "The branches have diverged, so a non-fast-forward merge path is required.";
        }
        return "The source branch is already mergeable with the target branch.";
    }
}
