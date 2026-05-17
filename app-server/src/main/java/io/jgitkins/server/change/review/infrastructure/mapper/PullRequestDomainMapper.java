package io.jgitkins.server.change.review.infrastructure.mapper;

import io.jgitkins.server.domain.model.vo.CommitHash;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.infrastructure.persistence.model.PullRequestEntity;
import org.springframework.stereotype.Component;

@Component
public class PullRequestDomainMapper {

    public PullRequest toDomain(PullRequestEntity entity) {
        return PullRequest.rehydrate(
                PullRequestId.of(entity.getId()),
                RepositoryId.of(entity.getRepositoryId()),
                BranchHeadSnapshot.of(entity.getSourceBranch(), entity.getSourceHead()),
                BranchHeadSnapshot.of(entity.getTargetBranch(), entity.getTargetHead()),
                PullRequestStatus.valueOf(entity.getStatus()),
                null,
                toTargetDrift(entity),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public PullRequestEntity toEntity(PullRequest pullRequest) {
        PullRequestEntity entity = new PullRequestEntity();
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

    private TargetDrift toTargetDrift(PullRequestEntity entity) {
        if (!Boolean.TRUE.equals(entity.getTargetDrifted())) {
            return TargetDrift.none();
        }
        return TargetDrift.detected(
                CommitHash.of(entity.getPreviousTargetHead()),
                CommitHash.of(entity.getCurrentTargetHead()));
    }

    private void applyTargetDrift(PullRequestEntity entity, TargetDrift targetDrift) {
        entity.setTargetDrifted(targetDrift.drifted());
        if (targetDrift.drifted()) {
            entity.setPreviousTargetHead(targetDrift.previousTargetHead().getValue());
            entity.setCurrentTargetHead(targetDrift.currentTargetHead().getValue());
            return;
        }
        entity.setPreviousTargetHead(null);
        entity.setCurrentTargetHead(null);
    }
}
