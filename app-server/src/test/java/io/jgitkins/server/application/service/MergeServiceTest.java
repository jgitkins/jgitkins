package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.application.port.out.MergeGitPort;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.model.changegraph.MergeabilityStatus;
import io.jgitkins.server.domain.model.changegraph.MergeTopologySummary;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

    @Mock
    private MergeGitPort mergeGitPort;

    @Mock
    private MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;

    @InjectMocks
    private MergeService service;

    @Test
    void checkMergeability_delegatesToPort() throws IOException {
        MergeResult result = MergeResult.builder().build();
        when(mergeGitPort.previewMergeability("task", "repo", "src", "dst")).thenReturn(result);

        MergeResult response = service.checkMergeability("task", "repo", "src", "dst");

        assertEquals(result, response);
    }

    @Test
    void performMerge_delegatesToPort() throws IOException {
        MergeResult result = MergeResult.builder().build();
        MergeRequest request = new MergeRequest();
        when(mergeGitPort.merge("task", "repo", request)).thenReturn(result);

        MergeResult response = service.performMerge("task", "repo", request);

        assertEquals(result, response);
    }

    @Test
    void evaluateMergeability_mapsPreviewResultToAssessment() throws IOException {
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGEABLE)
                .build();
        MergeabilityAssessment assessment = new MergeabilityAssessment(
                MergeabilityStatus.MERGEABLE,
                MergeTopologySummary.unknown(),
                null,
                "mergeable");

        when(mergeGitPort.previewMergeability("task", "repo", "src", "dst")).thenReturn(result);
        when(mergeabilityAssessmentAssembler.toAssessment(result)).thenReturn(assessment);

        MergeabilityAssessment response = service.evaluateMergeability("task", "repo", "src", "dst");

        assertEquals(assessment, response);
    }
}
