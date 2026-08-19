package io.jgitkins.server.change.review.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.infrastructure.persistence.model.PullRequestEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PullRequestDomainMapperTest {

    private final PullRequestDomainMapper mapper = new PullRequestDomainMapper();

    @Test
    void toEntity_mapsPersistentPullRequestStateWithoutMergeabilityAssessment() {
        PullRequest pullRequest = PullRequest.create(
                        ReviewRepositoryId.of(1L),
                        BranchHeadSnapshot.of("feature", "aaaaaaa"),
                        BranchHeadSnapshot.of("main", "bbbbbbb"))
                .withIdentity(PullRequestId.of(10L), LocalDateTime.now().minusDays(1), LocalDateTime.now())
                .markTargetDrifted(BranchHeadSnapshot.of("main", "ccccccc"));

        PullRequestEntity entity = mapper.toEntity(pullRequest);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getRepositoryId()).isEqualTo(1L);
        assertThat(entity.getSourceBranch()).isEqualTo("feature");
        assertThat(entity.getSourceHead()).isEqualTo("aaaaaaa");
        assertThat(entity.getTargetBranch()).isEqualTo("main");
        assertThat(entity.getTargetHead()).isEqualTo("ccccccc");
        assertThat(entity.getStatus()).isEqualTo("OPEN");
        assertThat(entity.getTargetDrifted()).isTrue();
        assertThat(entity.getPreviousTargetHead()).isEqualTo("bbbbbbb");
        assertThat(entity.getCurrentTargetHead()).isEqualTo("ccccccc");
    }

    @Test
    void toDomain_restoresPullRequestWithoutAssessmentSnapshot() {
        PullRequestEntity entity = new PullRequestEntity();
        entity.setId(10L);
        entity.setRepositoryId(1L);
        entity.setSourceBranch("feature");
        entity.setSourceHead("aaaaaaa");
        entity.setTargetBranch("main");
        entity.setTargetHead("bbbbbbb");
        entity.setStatus("OPEN");
        entity.setTargetDrifted(false);
        entity.setCreatedAt(LocalDateTime.now().minusDays(1));
        entity.setUpdatedAt(LocalDateTime.now());

        PullRequest pullRequest = mapper.toDomain(entity);

        assertThat(pullRequest.getId()).isEqualTo(PullRequestId.of(10L));
        assertThat(pullRequest.getRepositoryId()).isEqualTo(ReviewRepositoryId.of(1L));
        assertThat(pullRequest.getLastAssessmentSnapshot()).isNull();
        assertThat(pullRequest.getTargetDrift()).isEqualTo(TargetDrift.none());
    }
}
