package io.jgitkins.server.change.review.adapter.out.acl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jgitkins.server.change.review.application.exception.BranchHeadNotFoundException;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import org.junit.jupiter.api.Test;

class BranchHeadAclAdapterTest {
    private final BranchGitPort git = mock(BranchGitPort.class);
    private final BranchHeadAclAdapter adapter = new BranchHeadAclAdapter(git);
    private final ReviewRepositoryReference repository = new ReviewRepositoryReference(ReviewRepositoryId.of(1L), "alice", "demo");

    @Test
    void readsHeadUsingReferenceNamespaceAndName() {
        when(git.getHeadCommitHash("alice", "demo", "main")).thenReturn("abc1234");
        BranchHeadSnapshot result = adapter.getCurrentHead(repository, "main");
        assertEquals("main", result.branchName().getValue());
        assertEquals("abc1234", result.commitHash().getValue());
        verify(git).getHeadCommitHash("alice", "demo", "main");
    }

    @Test
    void translatesOnlyMissingBranch() {
        GitBranchRefMissingException missing = mock(GitBranchRefMissingException.class);
        when(missing.getBranchName()).thenReturn("missing");
        when(git.getHeadCommitHash("alice", "demo", "missing")).thenThrow(missing);
        BranchHeadNotFoundException exception = assertThrows(BranchHeadNotFoundException.class,
                () -> adapter.getCurrentHead(repository, "missing"));
        assertEquals("Branch not found: missing", exception.getMessage());
    }

    @Test
    void propagatesOtherInfrastructureFailures() {
        RuntimeException failure = new RuntimeException("git unavailable");
        when(git.getHeadCommitHash("alice", "demo", "main")).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class, () -> adapter.getCurrentHead(repository, "main")));
    }
}
