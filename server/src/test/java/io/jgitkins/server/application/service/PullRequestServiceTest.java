package io.jgitkins.server.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.application.dto.result.PullRequestResult;
import io.jgitkins.server.repository.application.port.out.BranchGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.support.pr.PullRequestDetailMapper;
import io.jgitkins.server.application.support.pr.PullRequestMergeabilityResolver;
import io.jgitkins.server.application.support.pr.PullRequestResultMapper;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.model.changegraph.MergeabilityStatus;
import io.jgitkins.server.domain.model.changegraph.MergeTopologySummary;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.BranchHeadSnapshot;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;
import io.jgitkins.server.domain.pr.repository.PullRequestRepository;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTest {

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private RepositoryPersistencePort repositoryPersistencePort;

    @Mock
    private RepositoryLookupService repositoryLookupService;

    @Mock
    private RepositoryNamespaceResolver repositoryNamespaceResolver;

    @Mock
    private BranchGitPort branchGitPort;

    @Mock
    private PullRequestMergeabilityResolver mergeabilityResolver;

    private PullRequestService service;

    @BeforeEach
    void setUp() {
        service = new PullRequestService(
                pullRequestRepository,
                repositoryPersistencePort,
                repositoryLookupService,
                repositoryNamespaceResolver,
                branchGitPort,
                mergeabilityResolver,
                new PullRequestResultMapper(),
                new PullRequestDetailMapper());
    }

    @Test
    void createPullRequest_persistsInitialSnapshotsWithoutMergeabilityAssessment() throws Exception {
        PullRequestCreateCommand command = new PullRequestCreateCommand("demo-org", "demo", "feature", "main");
        Repository repository = repository();

        when(repositoryLookupService.resolveByPath("demo-org", "demo")).thenReturn(Optional.of(repository));
        when(repositoryNamespaceResolver.resolve(repository)).thenReturn("demo-org");
        when(branchGitPort.getHeadCommitHash("demo-org", "demo", "feature")).thenReturn("aaaaaaa");
        when(branchGitPort.getHeadCommitHash("demo-org", "demo", "main")).thenReturn("bbbbbbb");
        when(pullRequestRepository.save(any())).thenAnswer(invocation -> {
            PullRequest pullRequest = invocation.getArgument(0);
            return pullRequest.withIdentity(PullRequestId.of(10L), pullRequest.getCreatedAt(), pullRequest.getUpdatedAt());
        });

        PullRequestResult result = service.createPullRequest(command);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getRepositoryId()).isEqualTo(1L);
        assertThat(result.getSource().branchName().getValue()).isEqualTo("feature");
        assertThat(result.getSource().commitHash().getValue()).isEqualTo("aaaaaaa");
        assertThat(result.getTarget().branchName().getValue()).isEqualTo("main");
        assertThat(result.getTarget().commitHash().getValue()).isEqualTo("bbbbbbb");
        verify(pullRequestRepository).save(any(PullRequest.class));
        verify(mergeabilityResolver, never()).assess(any(), any());
    }

    @Test
    void getPullRequestDetail_recalculatesMergeabilityWithoutSavingAssessmentSnapshot() throws Exception {
        PullRequestId pullRequestId = PullRequestId.of(10L);
        PullRequest pullRequest = PullRequest.create(
                        RepositoryId.of(1L),
                        BranchHeadSnapshot.of("feature", "aaaaaaa"),
                        BranchHeadSnapshot.of("main", "bbbbbbb"))
                .withIdentity(pullRequestId, null, null);
        Repository repository = repository();
        BranchHeadSnapshot currentSource = BranchHeadSnapshot.of("feature", "aaaaaaa");
        BranchHeadSnapshot currentTarget = BranchHeadSnapshot.of("main", "bbbbbbb");
        MergeabilityAssessment assessment = new MergeabilityAssessment(
                MergeabilityStatus.MERGEABLE,
                MergeTopologySummary.known(true, false),
                null,
                "mergeable");

        when(pullRequestRepository.findById(pullRequestId)).thenReturn(Optional.of(pullRequest));
        when(repositoryPersistencePort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(mergeabilityResolver.currentSourceHead(repository, pullRequest)).thenReturn(currentSource);
        when(mergeabilityResolver.currentTargetHead(repository, pullRequest)).thenReturn(currentTarget);
        when(mergeabilityResolver.assess(any(), any())).thenReturn(assessment);

        PullRequestDetailResult result = service.getPullRequestDetail(pullRequestId);

        assertThat(result.getMergeability()).isEqualTo(assessment);
        assertThat(result.getCurrentSource()).isEqualTo(currentSource);
        assertThat(result.getCurrentTarget()).isEqualTo(currentTarget);
        verify(pullRequestRepository, never()).save(any());
    }

    @Test
    void getPullRequestDetail_calculatesTargetDriftWithoutPersistingReadSideObservation() throws Exception {
        PullRequestId pullRequestId = PullRequestId.of(10L);
        PullRequest pullRequest = PullRequest.create(
                        RepositoryId.of(1L),
                        BranchHeadSnapshot.of("feature", "aaaaaaa"),
                        BranchHeadSnapshot.of("main", "bbbbbbb"))
                .withIdentity(pullRequestId, null, null);
        Repository repository = repository();
        BranchHeadSnapshot currentSource = BranchHeadSnapshot.of("feature", "aaaaaaa");
        BranchHeadSnapshot currentTarget = BranchHeadSnapshot.of("main", "ccccccc");
        MergeabilityAssessment assessment = new MergeabilityAssessment(
                MergeabilityStatus.MERGEABLE,
                MergeTopologySummary.known(false, true),
                null,
                "mergeable");

        when(pullRequestRepository.findById(pullRequestId)).thenReturn(Optional.of(pullRequest));
        when(repositoryPersistencePort.findById(RepositoryId.of(1L))).thenReturn(Optional.of(repository));
        when(mergeabilityResolver.currentSourceHead(repository, pullRequest)).thenReturn(currentSource);
        when(mergeabilityResolver.currentTargetHead(repository, pullRequest)).thenReturn(currentTarget);
        when(mergeabilityResolver.assess(any(), any())).thenReturn(assessment);

        PullRequestDetailResult result = service.getPullRequestDetail(pullRequestId);

        verify(pullRequestRepository, never()).save(any());
        assertThat(result.getTargetDrift().drifted()).isTrue();
        assertThat(result.getMergeability()).isEqualTo(assessment);
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
