package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.domain.model.OrganizeMember;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.OrganizeMemberDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeMemberEntity;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeMemberEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeMemberPersistenceAdapter implements OrganizeMemberPersistencePort {

    private final OrganizeMemberEntityMbgMapper organizeMemberMapper;

    private final OrganizeMemberDomainMapper organizeMemberDomainMapper;

    @Override
    public OrganizeMember save(OrganizeMember member) {
        try {
            OrganizeMemberEntity entity = organizeMemberDomainMapper.toEntity(member);
            organizeMemberMapper.insertSelective(entity);
            return organizeMemberDomainMapper.toDomain(entity);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save organize member", e);
        }
    }

    @Override
    public boolean existsByOrganizeIdAndUserId(OrganizeId organizeId, UserId userId) {
        try {
            OrganizeMemberEntityCondition condition = new OrganizeMemberEntityCondition();
            condition.createCriteria()
                    .andOrganizeIdEqualTo(organizeId.getValue())
                    .andUserIdEqualTo(userId.getValue());
            return organizeMemberMapper.countByCondition(condition) > 0;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during check organize member existence", e);
        }
    }

    @Override
    public Optional<OrganizeMember> findByOrganizeIdAndUserId(OrganizeId organizeId, UserId userId) {
        try {
            if (organizeId == null || userId == null) {
                return Optional.empty();
            }
            OrganizeMemberEntityCondition condition = new OrganizeMemberEntityCondition();
            condition.createCriteria()
                    .andOrganizeIdEqualTo(organizeId.getValue())
                    .andUserIdEqualTo(userId.getValue());
            return organizeMemberMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(organizeMemberDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize member", e);
        }
    }

    @Override
    public void deleteByOrganizeIdAndUserId(OrganizeId organizeId, UserId userId) {
        try {
            OrganizeMemberEntityCondition condition = new OrganizeMemberEntityCondition();
            condition.createCriteria()
                    .andOrganizeIdEqualTo(organizeId.getValue())
                    .andUserIdEqualTo(userId.getValue());
            organizeMemberMapper.deleteByCondition(condition);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete organize member", e);
        }
    }

    @Override
    public java.util.List<OrganizeMember> findAllByOrganizeId(OrganizeId organizeId) {
        try {
            OrganizeMemberEntityCondition condition = new OrganizeMemberEntityCondition();
            condition.createCriteria().andOrganizeIdEqualTo(organizeId.getValue());
            condition.setOrderByClause("joined_at desc");
            return organizeMemberMapper.selectByCondition(condition)
                    .stream()
                    .map(organizeMemberDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all organize members", e);
        }
    }
}
