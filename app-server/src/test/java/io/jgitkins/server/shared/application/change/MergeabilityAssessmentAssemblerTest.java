package io.jgitkins.server.shared.application.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.jgitkins.server.change.review.application.contract.MergeResult;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class MergeabilityAssessmentAssemblerTest {

    private final MergeabilityAssessmentAssembler assembler = new MergeabilityAssessmentAssembler();

    @Test
    void assemble_mapsFastForwardMergeableResult() {
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGEABLE)
                .fastForwardPossible(true)
                .mergeCommitRequired(false)
                .build();

        MergeabilityAssessment assessment = assembler.toAssessment(result);

        assertEquals(MergeabilityStatus.MERGEABLE, assessment.status());
        assertEquals(true, assessment.topology().fastForwardPossible());
        assertEquals(false, assessment.topology().mergeCommitRequired());
        assertEquals("The source branch is ahead of the target, so fast-forward is topologically possible.", assessment.reason());
    }

    @Test
    void assemble_mapsMergeCommitRequiredResult() {
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGEABLE)
                .fastForwardPossible(false)
                .mergeCommitRequired(true)
                .build();

        MergeabilityAssessment assessment = assembler.toAssessment(result);

        assertEquals(MergeabilityStatus.MERGEABLE, assessment.status());
        assertEquals(false, assessment.topology().fastForwardPossible());
        assertEquals(true, assessment.topology().mergeCommitRequired());
        assertEquals("The branches have diverged, so a non-fast-forward merge path is required.", assessment.reason());
    }

    @Test
    void assemble_mapsConflictsAndKeepsConflictPaths() {
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.CONFLICTS)
                .conflicts(List.of("src/App.java"))
                .fastForwardPossible(false)
                .mergeCommitRequired(false)
                .build();

        MergeabilityAssessment assessment = assembler.toAssessment(result);

        assertEquals(MergeabilityStatus.CONFLICTING, assessment.status());
        assertEquals(List.of("src/App.java"), assessment.conflicts());
        assertEquals("The source branch currently conflicts with the target branch.", assessment.reason());
    }

    @Test
    void assemble_mapsNullResultToUnknownAssessment() {
        MergeabilityAssessment assessment = assembler.toAssessment(null);

        assertEquals(MergeabilityStatus.UNKNOWN, assessment.status());
        assertFalse(assessment.conflicts().iterator().hasNext());
        assertEquals("Mergeability has not been evaluated yet.", assessment.reason());
    }
}
