package io.jgitkins.server.change.review.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import io.jgitkins.server.change.review.application.port.out.MergePort;
import io.jgitkins.server.change.review.application.port.out.RepositoryWriteAccessPort;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityStatus;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeTopologySummary;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

    @Mock
    private io.jgitkins.server.change.review.application.port.out.RepositoryReadAccessPort repositoryReadAccessPort;


    @Mock
    private MergePort mergePort;

    @Mock
    private MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;
    @Mock
    private RepositoryWriteAccessPort repositoryWriteAccessPort;

    @InjectMocks
    private MergeService service;

    @Test
    void checkMergeability_delegatesToPort() throws IOException {
        MergeResult result = MergeResult.builder().build();
        when(mergePort.previewMergeability("task", "repo", "src", "dst")).thenReturn(result);

        MergeResult response = service.checkMergeability("task", "repo", "src", "dst", null);

        assertEquals(result, response);
    }

    private static final long REQUESTER = 7L;

    @Test
    void performMerge_delegatesToPortOnceWriteAccessIsGranted() throws IOException {
        MergeResult result = MergeResult.builder().build();
        MergeRequest request = new MergeRequest();
        when(mergePort.merge("task", "repo", request)).thenReturn(result);

        MergeResult response = service.performMerge("task", "repo", request, REQUESTER);

        assertEquals(result, response);
        verify(repositoryWriteAccessPort).requireWriteAccess("task", "repo", REQUESTER);
    }

    // --- task 2.123: merging had no authorization at all --------------------------------------
    //
    // performMerge was a single delegation to the port. The controller passed no principal and the
    // use case signature had no actor, so any anonymous caller could move any branch of any
    // repository, private ones included.

    @Test
    void performMerge_doesNotTouchTheRepositoryWhenWriteAccessIsRefused() throws IOException {
        MergeRequest request = new MergeRequest();
        doThrow(new ApplicationException(ApplicationErrorCode.ACCESS_DENIED, "nope"))
                .when(repositoryWriteAccessPort).requireWriteAccess("task", "repo", REQUESTER);

        assertThrows(ApplicationException.class,
                () -> service.performMerge("task", "repo", request, REQUESTER));

        // The merge must not be attempted. A merge that has already moved the ref is not undone by
        // throwing afterwards.
        verify(mergePort, never()).merge(any(), any(), any());
    }

    @Test
    void performMerge_checksAccessBeforeMerging() throws IOException {
        MergeRequest request = new MergeRequest();
        when(mergePort.merge("task", "repo", request)).thenReturn(MergeResult.builder().build());

        service.performMerge("task", "repo", request, REQUESTER);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(repositoryWriteAccessPort, mergePort);
        order.verify(repositoryWriteAccessPort).requireWriteAccess("task", "repo", REQUESTER);
        order.verify(mergePort).merge("task", "repo", request);
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

        when(mergePort.previewMergeability("task", "repo", "src", "dst")).thenReturn(result);
        when(mergeabilityAssessmentAssembler.toAssessment(result)).thenReturn(assessment);

        MergeabilityAssessment response = service.evaluateMergeability("task", "repo", "src", "dst");

        assertEquals(assessment, response);
    }

    @org.junit.jupiter.api.Test
    void checkMergeabilityRefusesBeforeTouchingGitWhenTheRepositoryIsNotVisible() {
        // A merge preview is a diff of two branches, so it is a read of the repository. Without this
        // test the gate could be deleted and nothing would fail -- the other test passes a null
        // requester against a do-nothing mock, which a missing call looks identical to.
        RuntimeException denial = new RuntimeException("not visible");
        org.mockito.Mockito.doThrow(denial)
                .when(repositoryReadAccessPort).requireReadAccess("task", "repo", null);

        assertEquals(denial, org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.checkMergeability("task", "repo", "src", "dst", null)));

        org.mockito.Mockito.verifyNoInteractions(mergePort);
    }
}
