package io.jgitkins.server.change.review.application.support;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.change.review.application.exception.BranchHeadNotFoundException;
import io.jgitkins.server.change.review.application.port.out.BranchHeadPort;
import io.jgitkins.server.change.review.application.port.out.MergePort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PullRequestMergeabilityResolverTest {
    @Mock private BranchHeadPort branchHeadPort;
    @Mock private MergePort mergePort;
    @Mock private MergeabilityAssessmentAssembler assembler;
    private PullRequestMergeabilityResolver resolver;
    @BeforeEach void setUp() { resolver = new PullRequestMergeabilityResolver(branchHeadPort, mergePort, assembler); }
    @Test void currentSourceHead_translatesMissingBranch() {
        ReviewRepositoryReference repository = new ReviewRepositoryReference(ReviewRepositoryId.of(1L), "demo-org", "demo");
        PullRequest pullRequest = PullRequest.create(ReviewRepositoryId.of(1L), BranchHeadSnapshot.of("feature", "aaaaaaa"), BranchHeadSnapshot.of("main", "bbbbbbb"));
        when(branchHeadPort.getCurrentHead(repository, "feature")).thenThrow(new BranchHeadNotFoundException("feature"));
        assertThrows(BranchHeadNotFoundException.class, () -> resolver.currentSourceHead(repository, pullRequest));
    }
}
