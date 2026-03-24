package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.RepositoryMemberDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.RepositoryMemberEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryMemberEntity;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryMemberEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryMemberPersistenceAdapter implements RepositoryMemberPersistencePort {

    private final RepositoryMemberEntityMbgMapper repositoryMemberEntityMbgMapper;
    private final RepositoryMemberDomainMapper repositoryMemberDomainMapper;

    @Override
    public boolean existsByRepositoryIdAndUserId(RepositoryId repositoryId, UserId userId) {
        try {
            if (repositoryId == null || userId == null) {
                return false;
            }
            RepositoryMemberEntityCondition condition = new RepositoryMemberEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(repositoryId.getValue())
                    .andUserIdEqualTo(userId.getValue());
            return repositoryMemberEntityMbgMapper.countByCondition(condition) > 0;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during check repository member existence", e);
        }
    }

    @Override
    public Optional<RepositoryMember> findByRepositoryIdAndUserId(RepositoryId repositoryId, UserId userId) {
        try {
            if (repositoryId == null || userId == null) {
                return Optional.empty();
            }
            RepositoryMemberEntityCondition condition = new RepositoryMemberEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(repositoryId.getValue())
                    .andUserIdEqualTo(userId.getValue());
            return repositoryMemberEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(repositoryMemberDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository member", e);
        }
    }

    @Override
    public RepositoryMember save(RepositoryMember member) {
        try {
            RepositoryMemberEntity entity = repositoryMemberDomainMapper.toEntity(member);
            repositoryMemberEntityMbgMapper.insertSelective(entity);
            return repositoryMemberDomainMapper.toDomain(entity);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save repository member", e);
        }
    }

    @Override
    public void deleteByRepositoryIdAndUserId(RepositoryId repositoryId, UserId userId) {
        try {
            RepositoryMemberEntityCondition condition = new RepositoryMemberEntityCondition();
            condition.createCriteria()
                    .andRepositoryIdEqualTo(repositoryId.getValue())
                    .andUserIdEqualTo(userId.getValue());
            repositoryMemberEntityMbgMapper.deleteByCondition(condition);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete repository member", e);
        }
    }

    @Override
    public java.util.List<RepositoryMember> findAllByRepositoryId(RepositoryId repositoryId) {
        try {
            RepositoryMemberEntityCondition condition = new RepositoryMemberEntityCondition();
            condition.createCriteria().andRepositoryIdEqualTo(repositoryId.getValue());
            condition.setOrderByClause("added_at desc");
            return repositoryMemberEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .map(repositoryMemberDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all repository members", e);
        }
    }

}
