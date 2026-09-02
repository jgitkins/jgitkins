package io.jgitkins.server.repository.adapter.out.persistence;

import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryMemberDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.RepositoryMemberEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.model.RepositoryMemberEntity;
import io.jgitkins.server.repository.adapter.out.persistence.model.RepositoryMemberEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Registered by {@code RepositoryPersistenceSelectorConfiguration}, not by component scanning.
 *
 * <p>The {@code @Component} annotation was removed in task 2.72: with a JPA implementation of the
 * same port on the classpath, scanning would register two candidates and the injection point would
 * be ambiguous. The composition root now names exactly one.
 */
@RequiredArgsConstructor
public class RepositoryMemberPersistenceAdapter implements RepositoryMemberPersistencePort {

    private final RepositoryMemberEntityMbgMapper repositoryMemberEntityMbgMapper;
    private final RepositoryMemberDomainMapper repositoryMemberDomainMapper;

    @Override
    public boolean existsByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId) {
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
    public Optional<RepositoryMember> findByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId) {
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
    public void deleteByRepositoryIdAndUserId(RepositoryId repositoryId, RepositoryMemberUserId userId) {
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
