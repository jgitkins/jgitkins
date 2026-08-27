package io.jgitkins.server.change.review.adapter.out.persistence.jpa;

import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.shared.domain.model.vo.CommitHash;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * JPA implementation of {@link PullRequestRepository}.
 *
 * <p>{@code lastAssessmentSnapshot} is rehydrated as {@code null}, matching the MyBatis mapper. The
 * assessment is not persisted at all — it is recomputed per read — so passing anything else here would
 * be inventing state the table does not hold.
 *
 * <p>One storage-level difference from MyBatis, with no observable effect: when drift clears, this
 * adapter writes {@code PREVIOUS_TARGET_HEAD} and {@code CURRENT_TARGET_HEAD} back to null, whereas
 * {@code updateByPrimaryKeySelective} omitted the null columns and left the previous values in place.
 * The read side gates both columns behind {@code TARGET_DRIFTED}, so stale values were never
 * observable either way; JPA simply also clears them. Recorded because the row contents genuinely
 * differ between providers, and someone comparing two databases would otherwise think one is wrong.
 */
@RequiredArgsConstructor
public class PullRequestJpaPersistenceAdapter implements PullRequestRepository {

    private final PullRequestJpaRepository pullRequestJpaRepository;

    @Override
    public PullRequest save(PullRequest pullRequest) {
        try {
            boolean creating = pullRequest.getId() == null;
            LocalDateTime now = LocalDateTime.now();

            PullRequestJpaEntity entity = toEntity(pullRequest);
            if (creating) {
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
            } else {
                entity.setUpdatedAt(now);
            }

            PullRequestJpaEntity saved = pullRequestJpaRepository.save(entity);

            if (creating) {
                return pullRequest.withIdentity(
                        PullRequestId.of(saved.getId()), saved.getCreatedAt(), saved.getUpdatedAt());
            }
            // The created-at comes from the aggregate, not the row: the update path never rewrites it,
            // and re-reading it would be a second query for a value the caller already holds.
            return pullRequest.withIdentity(
                    pullRequest.getId(), pullRequest.getCreatedAt(), saved.getUpdatedAt());
        } catch (Exception e) {
            throw persistence("Database operation failed during save pull request", e);
        }
    }

    @Override
    public Optional<PullRequest> findById(PullRequestId id) {
        try {
            return pullRequestJpaRepository.findById(id.value()).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find pull request by id", e);
        }
    }

    private PullRequestJpaEntity toEntity(PullRequest pullRequest) {
        PullRequestJpaEntity entity = new PullRequestJpaEntity();
        if (pullRequest.getId() != null) {
            entity.setId(pullRequest.getId().value());
        }
        entity.setRepositoryId(pullRequest.getRepositoryId().getValue());
        entity.setSourceBranch(pullRequest.getSource().branchName().getValue());
        entity.setSourceHead(pullRequest.getSource().commitHash().getValue());
        entity.setTargetBranch(pullRequest.getTarget().branchName().getValue());
        entity.setTargetHead(pullRequest.getTarget().commitHash().getValue());
        entity.setStatus(pullRequest.getStatus().name());
        entity.setCreatedAt(pullRequest.getCreatedAt());
        entity.setUpdatedAt(pullRequest.getUpdatedAt());
        applyTargetDrift(entity, pullRequest.getTargetDrift());
        return entity;
    }

    private void applyTargetDrift(PullRequestJpaEntity entity, TargetDrift targetDrift) {
        entity.setTargetDrifted(targetDrift.drifted());
        if (targetDrift.drifted()) {
            entity.setPreviousTargetHead(targetDrift.previousTargetHead().getValue());
            entity.setCurrentTargetHead(targetDrift.currentTargetHead().getValue());
            return;
        }
        entity.setPreviousTargetHead(null);
        entity.setCurrentTargetHead(null);
    }

    private PullRequest toDomain(PullRequestJpaEntity entity) {
        return PullRequest.rehydrate(
                PullRequestId.of(entity.getId()),
                ReviewRepositoryId.of(entity.getRepositoryId()),
                BranchHeadSnapshot.of(entity.getSourceBranch(), entity.getSourceHead()),
                BranchHeadSnapshot.of(entity.getTargetBranch(), entity.getTargetHead()),
                PullRequestStatus.valueOf(entity.getStatus()),
                null,
                toTargetDrift(entity),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private TargetDrift toTargetDrift(PullRequestJpaEntity entity) {
        // TARGET_DRIFTED is the gate, not the head columns. A row can carry stale heads with the flag
        // clear -- the MyBatis update path leaves them behind -- and reading them would resurrect drift
        // that no longer exists.
        if (!entity.isTargetDrifted()) {
            return TargetDrift.none();
        }
        return TargetDrift.detected(
                CommitHash.of(entity.getPreviousTargetHead()),
                CommitHash.of(entity.getCurrentTargetHead()));
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
