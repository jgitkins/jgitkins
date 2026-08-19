package io.jgitkins.server.change.review.domain.aggregate;

import io.jgitkins.server.shared.domain.aggregate.AbstractAggregateRoot;
import io.jgitkins.server.change.review.domain.model.changegraph.MergeabilityAssessment;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PullRequest extends AbstractAggregateRoot<PullRequestId> {

    private final PullRequestId id;
    private final ReviewRepositoryId repositoryId;
    private final BranchHeadSnapshot source;
    private final BranchHeadSnapshot target;
    private final PullRequestStatus status;
    private final MergeabilityAssessment lastAssessmentSnapshot;
    private final TargetDrift targetDrift;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PullRequest(PullRequestId id,
                        ReviewRepositoryId repositoryId,
                        BranchHeadSnapshot source,
                        BranchHeadSnapshot target,
                        PullRequestStatus status,
                        MergeabilityAssessment lastAssessmentSnapshot,
                        TargetDrift targetDrift,
                        LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        if (repositoryId == null) {
            throw new IllegalArgumentException("ReviewRepositoryId must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source snapshot must not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target snapshot must not be null");
        }
        if (source.hasSameBranch(target)) {
            throw new IllegalArgumentException("Source and target branches must be different");
        }

        LocalDateTime effectiveCreatedAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.id = id;
        this.repositoryId = repositoryId;
        this.source = source;
        this.target = target;
        this.status = status != null ? status : PullRequestStatus.OPEN;
        this.lastAssessmentSnapshot = lastAssessmentSnapshot;
        this.targetDrift = targetDrift != null ? targetDrift : TargetDrift.none();
        this.createdAt = effectiveCreatedAt;
        this.updatedAt = updatedAt != null ? updatedAt : effectiveCreatedAt;
    }

    public static PullRequest create(ReviewRepositoryId repositoryId,
                                     BranchHeadSnapshot source,
                                     BranchHeadSnapshot target) {
        LocalDateTime now = LocalDateTime.now();
        return new PullRequest(
                null,
                repositoryId,
                source,
                target,
                PullRequestStatus.OPEN,
                null,
                TargetDrift.none(),
                now,
                now);
    }

    public static PullRequest rehydrate(PullRequestId id,
                                        ReviewRepositoryId repositoryId,
                                        BranchHeadSnapshot source,
                                        BranchHeadSnapshot target,
                                        PullRequestStatus status,
                                        MergeabilityAssessment lastAssessmentSnapshot,
                                        TargetDrift targetDrift,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        return new PullRequest(
                id,
                repositoryId,
                source,
                target,
                status,
                lastAssessmentSnapshot,
                targetDrift,
                createdAt,
                updatedAt);
    }

    public PullRequest withIdentity(PullRequestId id,
                                    LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
        PullRequest identified = new PullRequest(
                id,
                repositoryId,
                source,
                target,
                status,
                lastAssessmentSnapshot,
                targetDrift,
                createdAt,
                updatedAt);
        identified.copyDomainEventsFrom(this);
        return identified;
    }

    public PullRequest updateSource(BranchHeadSnapshot newSource) {
        requireOpen("update source");
        return copy(newSource, target, status, lastAssessmentSnapshot, targetDrift);
    }

    public PullRequest recordAssessmentSnapshot(MergeabilityAssessment assessment) {
        requireOpen("record mergeability assessment");
        return copy(source, target, status, assessment, targetDrift);
    }

    public PullRequest markTargetDrifted(BranchHeadSnapshot currentTarget) {
        requireOpen("mark target drifted");
        if (currentTarget == null) {
            throw new IllegalArgumentException("Current target snapshot must not be null");
        }
        if (!target.hasSameBranch(currentTarget)) {
            throw new IllegalArgumentException("Current target branch must match pull request target branch");
        }
        if (target.commitHash().equals(currentTarget.commitHash())) {
            return copy(source, currentTarget, status, lastAssessmentSnapshot, TargetDrift.none());
        }
        TargetDrift drift = TargetDrift.detected(target.commitHash(), currentTarget.commitHash());
        return copy(source, currentTarget, status, lastAssessmentSnapshot, drift);
    }

    public PullRequest close() {
        requireOpen("close");
        return copy(source, target, PullRequestStatus.CLOSED, lastAssessmentSnapshot, targetDrift);
    }

    public PullRequest reopen() {
        if (status != PullRequestStatus.CLOSED) {
            throw new IllegalStateException("Only closed pull requests can be reopened");
        }
        return copy(source, target, PullRequestStatus.OPEN, lastAssessmentSnapshot, targetDrift);
    }

    public PullRequest markMerged() {
        requireOpen("mark merged");
        return copy(source, target, PullRequestStatus.MERGED, lastAssessmentSnapshot, targetDrift);
    }

    public boolean isOpen() {
        return status == PullRequestStatus.OPEN;
    }

    private void requireOpen(String action) {
        if (!isOpen()) {
            throw new IllegalStateException("Only open pull requests can " + action);
        }
    }

    private PullRequest copy(BranchHeadSnapshot source,
                             BranchHeadSnapshot target,
                             PullRequestStatus status,
                             MergeabilityAssessment lastAssessmentSnapshot,
                             TargetDrift targetDrift) {
        PullRequest copied = new PullRequest(
                id,
                repositoryId,
                source,
                target,
                status,
                lastAssessmentSnapshot,
                targetDrift,
                createdAt,
                LocalDateTime.now());
        copied.copyDomainEventsFrom(this);
        return copied;
    }
}
