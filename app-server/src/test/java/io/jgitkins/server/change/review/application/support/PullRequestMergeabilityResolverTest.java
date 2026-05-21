package io.jgitkins.server.change.review.application.support;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.change.review.application.port.out.MergeGitPort;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitBranchRefMissingException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.application.change.MergeabilityAssessmentAssembler;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PullRequestMergeabilityResolverTest {

    @Mock
    private BranchGitPort branchGitPort;

    @Mock
    private MergeGitPort mergeGitPort;

    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;

    @Mock
    private MergeabilityAssessmentAssembler mergeabilityAssessmentAssembler;

    private PullRequestMergeabilityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PullRequestMergeabilityResolver(
                branchGitPort,
                mergeGitPort,
                repositoryNamespaceResolver,
                mergeabilityAssessmentAssembler);
    }

    @Test
    void currentSourceHead_translatesMissingGitBranchRefToBranchNotFound() {
        Repository repository = repository();
        PullRequest pullRequest = PullRequest.create(
                RepositoryId.of(1L),
                BranchHeadSnapshot.of("feature", "aaaaaaa"),
                BranchHeadSnapshot.of("main", "bbbbbbb"));

        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("demo-org");
        when(branchGitPort.getHeadCommitHash("demo-org", "demo", "feature"))
                .thenThrow(new GitBranchRefMissingException("feature"));

        assertThrows(BranchNotFoundException.class, () -> resolver.currentSourceHead(repository, pullRequest));
    }

    private Repository repository() {
        return Repository.create(
                        null,
                        null,
                        RepositoryName.from("demo"),
                        RepositoryPath.from("demo"),
                        BranchName.of("main"),
                        RepositoryVisibility.PRIVATE,
                        null,
                        "/demo/demo.git",
                        null,
                        false)
                .withIdentity(RepositoryId.of(1L), null, null);
    }
}
