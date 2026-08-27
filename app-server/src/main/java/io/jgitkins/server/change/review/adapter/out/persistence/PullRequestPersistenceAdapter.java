package io.jgitkins.server.change.review.adapter.out.persistence;

import io.jgitkins.server.change.review.domain.aggregate.PullRequest;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.change.review.domain.repository.PullRequestRepository;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.change.review.infrastructure.mapper.PullRequestDomainMapper;
import io.jgitkins.server.change.review.infrastructure.persistence.mapper.PullRequestEntityMbgMapper;
import io.jgitkins.server.change.review.infrastructure.persistence.model.PullRequestEntity;
import io.jgitkins.server.change.review.infrastructure.persistence.model.PullRequestEntityCondition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Registered by {@code ChangeReviewPersistenceSelectorConfiguration}, not by component scanning.
 *
 * <p>The {@code @Component} annotation was removed in task 2.75: with a JPA implementation of
 * {@code PullRequestRepository} on the classpath, scanning would register two candidates and the
 * injection point would be ambiguous.
 */
@RequiredArgsConstructor
public class PullRequestPersistenceAdapter implements PullRequestRepository {

    private final PullRequestEntityMbgMapper mapper;
    private final PullRequestDomainMapper domainMapper;

    @Override
    public PullRequest save(PullRequest pullRequest) {
        try {
            PullRequestEntity entity = domainMapper.toEntity(pullRequest);
            if (pullRequest.getId() == null) {
                LocalDateTime now = LocalDateTime.now();
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                mapper.insertSelective(entity);
                return pullRequest.withIdentity(PullRequestId.of(entity.getId()), entity.getCreatedAt(), entity.getUpdatedAt());
            }

            entity.setUpdatedAt(LocalDateTime.now());
            mapper.updateByPrimaryKeySelective(entity);
            return pullRequest.withIdentity(pullRequest.getId(), pullRequest.getCreatedAt(), entity.getUpdatedAt());
        } catch (Exception e) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save pull request",
                    e);
        }
    }

    @Override
    public Optional<PullRequest> findById(PullRequestId id) {
        try {
            PullRequestEntityCondition condition = new PullRequestEntityCondition();
            condition.createCriteria().andIdEqualTo(id.value());
            List<PullRequestEntity> entities = mapper.selectByCondition(condition);
            return entities.stream().findFirst().map(domainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find pull request by id",
                    e);
        }
    }
}
