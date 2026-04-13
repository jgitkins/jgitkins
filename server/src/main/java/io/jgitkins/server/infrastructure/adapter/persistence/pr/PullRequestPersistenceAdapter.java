package io.jgitkins.server.infrastructure.adapter.persistence.pr;

import io.jgitkins.server.domain.pr.aggregate.PullRequest;
import io.jgitkins.server.domain.pr.model.vo.PullRequestId;
import io.jgitkins.server.domain.pr.repository.PullRequestRepository;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.PullRequestDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.PullRequestEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.PullRequestEntity;
import io.jgitkins.server.infrastructure.persistence.model.PullRequestEntityCondition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
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
