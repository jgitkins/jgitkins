package io.jgitkins.server.change.review.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.change.review.application.contract.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.mapper.PullRequestDetailMapper;
import io.jgitkins.server.change.review.application.port.out.RepositoryReadAccessPort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.application.support.PullRequestMergeabilityResolver;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PullRequestQueryServiceTest {
    private final PullRequestRepository pullRequests = mock(PullRequestRepository.class);
    private final RepositoryReferencePort references = mock(RepositoryReferencePort.class);
    private final PullRequestMergeabilityResolver resolver = mock(PullRequestMergeabilityResolver.class);
    private final PullRequestDetailMapper details = mock(PullRequestDetailMapper.class);
    private final RepositoryReadAccessPort readAccess = mock(RepositoryReadAccessPort.class);
    private static final Long REQUESTER = 7L;
    private final PullRequestQueryService service = new PullRequestQueryService(pullRequests, references, readAccess, resolver, details);
    private final ReviewRepositoryReference repository = new ReviewRepositoryReference(ReviewRepositoryId.of(1L), "alice", "demo");

    @Test
    void detailLoadsAggregateObservesHeadsAssessesAndMaps() throws Exception {
        PullRequestId id = PullRequestId.of(5L);
        PullRequest stored = PullRequest.rehydrate(id, repository.id(), BranchHeadSnapshot.of("feature", "feature-head"),
                BranchHeadSnapshot.of("main", "base-head"), null, null, null, null, null);
        BranchHeadSnapshot source = BranchHeadSnapshot.of("feature", "feature-head");
        BranchHeadSnapshot target = BranchHeadSnapshot.of("main", "base-head");
        MergeabilityAssessment assessment = mock(MergeabilityAssessment.class);
        PullRequestDetailResult result = PullRequestDetailResult.builder().id(5L).repositoryId(1L).build();
        when(pullRequests.findById(id)).thenReturn(Optional.of(stored));
        when(references.findById(repository.id())).thenReturn(Optional.of(repository));
        when(resolver.currentSourceHead(repository, stored)).thenReturn(source);
        when(resolver.currentTargetHead(repository, stored)).thenReturn(target);
        when(resolver.assess(eq(repository), any(PullRequest.class))).thenReturn(assessment);
        when(details.toDetail(any(PullRequest.class), eq(source), eq(target), eq(assessment))).thenReturn(result);

        assertSame(result, service.getPullRequestDetail(id, REQUESTER));
        verify(details).toDetail(any(PullRequest.class), eq(source), eq(target), eq(assessment));
        verify(resolver).assess(eq(repository), any(PullRequest.class));
    }

    @Test
    void aRepositoryTheCallerCannotSeeIsRefusedBeforeTheMergeabilityWork() {
        // The guard sits after the repository is resolved and before the branch heads are read off
        // disk. Asserting the resolver was never touched is what proves the ordering: a check placed
        // after it would answer 404 having already done the work and read the branch state.
        PullRequest pullRequest = PullRequest.rehydrate(PullRequestId.of(5L), repository.id(),
                BranchHeadSnapshot.of("feature", "feature1"), BranchHeadSnapshot.of("main", "main123"),
                null, null, null, null, null);
        when(pullRequests.findById(PullRequestId.of(5L))).thenReturn(Optional.of(pullRequest));
        when(references.findById(repository.id())).thenReturn(Optional.of(repository));
        // A plain RuntimeException, not the repository context's concrete type. What this asserts is
        // the ordering -- the gate is consulted, and a throw from it aborts before any work runs.
        // Which exception the gate throws is that context's business and is asserted there; naming
        // the type here would also put a foreign import in a change/review test, which the
        // architecture guard reads as the same violation whether it is code or a doc comment.
        RuntimeException denial = new RuntimeException("not visible");
        doThrow(denial).when(readAccess).requireReadAccess("alice", "demo", null);

        assertSame(denial, assertThrows(RuntimeException.class,
                () -> service.getPullRequestDetail(PullRequestId.of(5L), null)));

        verifyNoInteractions(resolver, details);
    }

    @Test
    void missingRepositoryByIdUsesExactApplicationException() {
        PullRequest pullRequest = PullRequest.rehydrate(PullRequestId.of(5L), repository.id(),
                BranchHeadSnapshot.of("feature", "feature1"), BranchHeadSnapshot.of("main", "main123"), null, null, null, null, null);
        when(pullRequests.findById(PullRequestId.of(5L))).thenReturn(Optional.of(pullRequest));
        when(references.findById(repository.id())).thenReturn(Optional.empty());

        RepositoryReferenceNotFoundException exception = assertThrows(RepositoryReferenceNotFoundException.class,
                () -> service.getPullRequestDetail(PullRequestId.of(5L), REQUESTER));
        assertEquals("Repository not found: 1", exception.getMessage());
        verifyNoInteractions(resolver, details);
    }
}
