package io.jgitkins.server.domain.pr.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jgitkins.server.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.domain.model.changegraph.MergeabilityStatus;
import io.jgitkins.server.domain.model.changegraph.MergeTopologySummary;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.domain.pr.model.BranchHeadSnapshot;
import io.jgitkins.server.domain.pr.model.PullRequestStatus;
import io.jgitkins.server.domain.pr.model.TargetDrift;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PullRequestTest {

    @Test
    void create_opensPullRequestWithSourceAndTargetSnapshots() {
        PullRequest pullRequest = PullRequest.create(
                RepositoryId.of(1L),
                snapshot("feature", "aaaaaaa"),
                snapshot("dev", "bbbbbbb"));

        assertThat(pullRequest.getId()).isNull();
        assertThat(pullRequest.getRepositoryId()).isEqualTo(RepositoryId.of(1L));
        assertThat(pullRequest.getSource().branchName().getValue()).isEqualTo("feature");
        assertThat(pullRequest.getTarget().branchName().getValue()).isEqualTo("dev");
        assertThat(pullRequest.getStatus()).isEqualTo(PullRequestStatus.OPEN);
        assertThat(pullRequest.getLastAssessmentSnapshot()).isNull();
        assertThat(pullRequest.getTargetDrift()).isEqualTo(TargetDrift.none());
        assertThat(pullRequest.isOpen()).isTrue();
    }

    @Test
    void create_rejectsSameSourceAndTargetBranch() {
        BranchHeadSnapshot source = snapshot("feature", "aaaaaaa");
        BranchHeadSnapshot target = snapshot("feature", "bbbbbbb");

        assertThatThrownBy(() -> PullRequest.create(RepositoryId.of(1L), source, target))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");
    }

    @Test
    void closeAndReopen_changesStatusOnlyThroughAllowedTransitions() {
        PullRequest pullRequest = openPullRequest();

        PullRequest closed = pullRequest.close();
        PullRequest reopened = closed.reopen();

        assertThat(closed.getStatus()).isEqualTo(PullRequestStatus.CLOSED);
        assertThat(reopened.getStatus()).isEqualTo(PullRequestStatus.OPEN);
        assertThatThrownBy(closed::markMerged)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only open");
        assertThatThrownBy(pullRequest::reopen)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only closed");
    }

    @Test
    void markMerged_requiresOpenRoute() {
        PullRequest pullRequest = openPullRequest();

        PullRequest merged = pullRequest.markMerged();

        assertThat(merged.getStatus()).isEqualTo(PullRequestStatus.MERGED);
        assertThatThrownBy(merged::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only open");
    }

    @Test
    void recordAssessmentSnapshot_keepsAssessmentAsOptionalSnapshot() {
        PullRequest pullRequest = openPullRequest();
        MergeabilityAssessment assessment = new MergeabilityAssessment(
                MergeabilityStatus.MERGEABLE,
                MergeTopologySummary.known(true, false),
                null,
                "mergeability snapshot");

        PullRequest recorded = pullRequest.recordAssessmentSnapshot(assessment);

        assertThat(recorded.getLastAssessmentSnapshot()).isEqualTo(assessment);
        assertThat(recorded.getStatus()).isEqualTo(PullRequestStatus.OPEN);
    }

    @Test
    void markTargetDrifted_recordsPreviousAndCurrentTargetHeads() {
        PullRequest pullRequest = openPullRequest();
        BranchHeadSnapshot currentTarget = snapshot("dev", "ccccccc");

        PullRequest drifted = pullRequest.markTargetDrifted(currentTarget);

        assertThat(drifted.getTarget().commitHash().getValue()).isEqualTo("ccccccc");
        assertThat(drifted.getTargetDrift().drifted()).isTrue();
        assertThat(drifted.getTargetDrift().previousTargetHead().getValue()).isEqualTo("bbbbbbb");
        assertThat(drifted.getTargetDrift().currentTargetHead().getValue()).isEqualTo("ccccccc");
    }

    @Test
    void markTargetDrifted_rejectsDifferentTargetBranch() {
        PullRequest pullRequest = openPullRequest();

        assertThatThrownBy(() -> pullRequest.markTargetDrifted(snapshot("main", "ccccccc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target branch");
    }

    @Test
    void rehydrate_restoresPersistedRouteState() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        PullRequest pullRequest = PullRequest.rehydrate(
                PullRequestId.of(10L),
                RepositoryId.of(1L),
                snapshot("feature", "aaaaaaa"),
                snapshot("dev", "bbbbbbb"),
                PullRequestStatus.CLOSED,
                null,
                TargetDrift.none(),
                createdAt,
                updatedAt);

        assertThat(pullRequest.getId()).isEqualTo(PullRequestId.of(10L));
        assertThat(pullRequest.getStatus()).isEqualTo(PullRequestStatus.CLOSED);
        assertThat(pullRequest.getCreatedAt()).isEqualTo(createdAt);
        assertThat(pullRequest.getUpdatedAt()).isEqualTo(updatedAt);
    }

    private PullRequest openPullRequest() {
        return PullRequest.create(
                RepositoryId.of(1L),
                snapshot("feature", "aaaaaaa"),
                snapshot("dev", "bbbbbbb"));
    }

    private BranchHeadSnapshot snapshot(String branchName, String commitHash) {
        return BranchHeadSnapshot.of(branchName, commitHash);
    }
}
