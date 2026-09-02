package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import org.junit.jupiter.api.Test;

/**
 * What the JPA adapter itself decides: which timestamps it writes, and how it reports failure.
 *
 * <p>All four cases were only ever asserted against the MyBatis adapter, in
 * {@code PullRequestPersistenceAdapterTest}, and none of them is the aggregate's behaviour: both
 * implementations stamp {@code now()} themselves, both choose whether {@code createdAt} is part of
 * the write, and both decide what a store failure looks like to the caller. So none of it came along
 * with the provider swap. {@code PullRequestJpaMappingTest} asserts the timestamp columns are
 * writable, which is a different claim from what gets written to them, and nothing at all asserted
 * the JPA adapter's failure reporting.
 *
 * <p>The failure cases were nearly lost. Sorting the MyBatis tests into "behaviour worth keeping"
 * and "mapper plumbing that dies with the provider" put the two exception-wrapping cases in the
 * second pile, on the assumption that the JPA adapter's equivalent was already covered somewhere. It
 * was not: {@code PERSISTENCE_OPERATION_FAILED} appeared in exactly one test file, the MyBatis one.
 *
 * <p>The update case is the one worth pinning. The JPA adapter deliberately returns the aggregate's
 * {@code createdAt} rather than the row's, on the reasoning that the update path never rewrites it
 * and re-reading would cost a query for a value the caller already holds. That is correct only while
 * the update really does leave the column alone; if a future mapping starts including
 * {@code createdAt} in the update, the returned aggregate would keep claiming the old value and
 * nothing else would notice.
 */
class PullRequestJpaAdapterContractTest {

    private final PullRequestJpaRepository repository = mock(PullRequestJpaRepository.class);
    private final PullRequestJpaPersistenceAdapter adapter = new PullRequestJpaPersistenceAdapter(repository);

    private final PullRequest pullRequest = PullRequest.create(ReviewRepositoryId.of(1L),
            BranchHeadSnapshot.of("feature", "feature-head"), BranchHeadSnapshot.of("main", "base-head"));

    @Test
    void insertStampsCreatedAndUpdatedToTheSameInstantAndTakesTheGeneratedId() {
        doAnswer(invocation -> {
            PullRequestJpaEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        }).when(repository).save(any(PullRequestJpaEntity.class));

        PullRequest saved = adapter.save(pullRequest);

        assertThat(saved.getId().value()).isEqualTo(42L);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt())
                .as("an insert has not been updated yet, so the two must be the same instant rather "
                        + "than merely close")
                .isEqualTo(saved.getCreatedAt());
    }

    @Test
    void wrapsARepositoryFailureAsPersistenceOperationFailedAndKeepsTheCause() {
        RuntimeException cause = new RuntimeException("insert failed");
        when(repository.save(any(PullRequestJpaEntity.class))).thenThrow(cause);

        assertThatThrownBy(() -> adapter.save(pullRequest))
                .isInstanceOf(InfrastructureException.class)
                .hasMessage("Database operation failed during save pull request")
                .hasFieldOrPropertyWithValue("errorCode", InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED)
                .hasCause(cause);
    }

    @Test
    void wrapsAReadFailureTheSameWay() {
        RuntimeException cause = new RuntimeException("select failed");
        when(repository.findById(1L)).thenThrow(cause);

        assertThatThrownBy(() -> adapter.findById(PullRequestId.of(1L)))
                .isInstanceOf(InfrastructureException.class)
                .hasMessage("Database operation failed during find pull request by id")
                .hasFieldOrPropertyWithValue("errorCode", InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED)
                .hasCause(cause);
    }

    @Test
    void updateRefreshesUpdatedAndLeavesCreatedAlone() {
        PullRequest existing = pullRequest.withIdentity(
                PullRequestId.of(7L), pullRequest.getCreatedAt(), pullRequest.getUpdatedAt());
        when(repository.save(any(PullRequestJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PullRequest updated = adapter.save(existing);

        assertThat(updated.getId().value()).isEqualTo(7L);
        assertThat(updated.getCreatedAt())
                .as("the update path must not rewrite createdAt; the adapter returns the aggregate's "
                        + "copy precisely because the row's is expected to be untouched")
                .isEqualTo(existing.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isNotNull();
    }
}
