package io.jgitkins.server.change.review.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.change.review.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.mapper.PullRequestDetailMapper;
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
    private final PullRequestQueryService service = new PullRequestQueryService(pullRequests, references, resolver, details);
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

        assertSame(result, service.getPullRequestDetail(id));
        verify(details).toDetail(any(PullRequest.class), eq(source), eq(target), eq(assessment));
        verify(resolver).assess(eq(repository), any(PullRequest.class));
    }

    @Test
    void missingRepositoryByIdUsesExactApplicationException() {
        PullRequest pullRequest = PullRequest.rehydrate(PullRequestId.of(5L), repository.id(),
                BranchHeadSnapshot.of("feature", "feature1"), BranchHeadSnapshot.of("main", "main123"), null, null, null, null, null);
        when(pullRequests.findById(PullRequestId.of(5L))).thenReturn(Optional.of(pullRequest));
        when(references.findById(repository.id())).thenReturn(Optional.empty());

        RepositoryReferenceNotFoundException exception = assertThrows(RepositoryReferenceNotFoundException.class,
                () -> service.getPullRequestDetail(PullRequestId.of(5L)));
        assertEquals("Repository not found: 1", exception.getMessage());
        verifyNoInteractions(resolver, details);
    }
}
