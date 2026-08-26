package io.jgitkins.server.collaboration.adapter.out.persistence;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeMemberDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeMemberEntityMbgMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeMemberEntity;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeMemberEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrganizeMemberPersistenceAdapter implements OrganizeMemberPersistencePort, OrganizeMembershipQueryPort {

    private final OrganizeMemberEntityMbgMapper organizeMemberMapper;

    private final OrganizeMemberDomainMapper organizeMemberDomainMapper;

    @Override
    public Optional<io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole> findRoleByOrganizeIdAndUserId(Long organizeId, Long userId) {
        try {
            if (organizeId == null || userId == null) return Optional.empty();
            OrganizeMemberEntityCondition condition = new OrganizeMemberEntityCondition();
            condition.createCriteria().andOrganizeIdEqualTo(organizeId).andUserIdEqualTo(userId);
            return organizeMemberMapper.selectByCondition(condition).stream().findFirst()
                    .map(entity -> io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole.from(entity.getRole()));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize member role", e);
        }
    }

    @Override
    public long countOwnersByOrganizeId(Long organizeId) {
        try {
            OrganizeMemberEntityCondition condition = new OrganizeMemberEntityCondition();
            condition.createCriteria().andOrganizeIdEqualTo(organizeId).andRoleEqualTo("OWNER");
            return organizeMemberMapper.countByCondition(condition);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during count organize owners", e);
        }
    }

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
    public boolean existsByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId) {
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
    public Optional<OrganizeMember> findByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId) {
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
    public void deleteByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId) {
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
