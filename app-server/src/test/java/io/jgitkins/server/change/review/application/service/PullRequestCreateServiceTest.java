package io.jgitkins.server.change.review.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.mapper.PullRequestResultMapper;
import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
import io.jgitkins.server.change.review.application.port.out.RepositoryWriteAccessPort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PullRequestCreateServiceTest {
    private final PullRequestRepository pullRequests = mock(PullRequestRepository.class);
    private final RepositoryReferencePort references = mock(RepositoryReferencePort.class);
    private final BranchHeadPort heads = mock(BranchHeadPort.class);
    private final PullRequestResultMapper results = mock(PullRequestResultMapper.class);
    private final RepositoryWriteAccessPort writeAccess = mock(RepositoryWriteAccessPort.class);
    private static final Long REQUESTER = 7L;
    private final PullRequestCreateService service = new PullRequestCreateService(pullRequests, references, heads, writeAccess, results);
    private final ReviewRepositoryReference repository = new ReviewRepositoryReference(ReviewRepositoryId.of(1L), "alice", "demo");
    private final PullRequestCreateCommand command = new PullRequestCreateCommand("alice", "demo", "feature", "main");

    @Test
    void createResolvesRepositoryReadsBothHeadsPersistsAndMaps() {
        BranchHeadSnapshot source = BranchHeadSnapshot.of("feature", "feature-head");
        BranchHeadSnapshot target = BranchHeadSnapshot.of("main", "base-head");
        PullRequest saved = PullRequest.create(repository.id(), source, target).withIdentity(
                io.jgitkins.server.change.review.domain.model.vo.PullRequestId.of(5L), null, null);
        PullRequestResult result = PullRequestResult.builder().id(5L).repositoryId(1L).build();
        when(references.findByPath("alice", "demo")).thenReturn(Optional.of(repository));
        when(heads.getCurrentHead(repository, "feature")).thenReturn(source);
        when(heads.getCurrentHead(repository, "main")).thenReturn(target);
        when(pullRequests.save(any(PullRequest.class))).thenReturn(saved);
        when(results.toResult(saved)).thenReturn(result);

        assertSame(result, service.createPullRequest(command, REQUESTER));
        verify(pullRequests).save(argThat(pr -> pr.getRepositoryId().equals(repository.id())
                && pr.getSource().equals(source) && pr.getTarget().equals(target)));
        verify(heads).getCurrentHead(repository, "feature");
        verify(heads).getCurrentHead(repository, "main");
    }

    @Test
    void missingRepositoryUsesExactApplicationException() {
        when(references.findByPath("alice", "demo")).thenReturn(Optional.empty());
        RepositoryReferenceNotFoundException exception = assertThrows(RepositoryReferenceNotFoundException.class,
                () -> service.createPullRequest(command, REQUESTER));
        assertEquals("Repository not found: alice/demo", exception.getMessage());
        verifyNoInteractions(heads, pullRequests, results);
    }

    @Test
    void aRepositoryTheRequesterMayNotWriteIsRefusedBeforeAnythingIsRead() {
        // Opening a pull request writes, so it answers to the commit gate. Without this test the
        // gate could be deleted and nothing would fail: the controller's 401 covers only the
        // anonymous case, and an authenticated non-writer would sail through.
        RuntimeException denial = new RuntimeException("not writable");
        doThrow(denial).when(writeAccess).requireWriteAccess("alice", "demo", REQUESTER);

        assertSame(denial, assertThrows(RuntimeException.class,
                () -> service.createPullRequest(command, REQUESTER)));

        // Before the lookup, not after: an unauthenticated or unauthorized caller must not learn
        // from the error whether the namespace and name resolve to anything.
        verifyNoInteractions(references, heads, pullRequests);
    }
}
