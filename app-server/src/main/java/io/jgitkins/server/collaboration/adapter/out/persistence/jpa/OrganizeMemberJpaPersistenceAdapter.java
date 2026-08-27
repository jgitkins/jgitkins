package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import io.jgitkins.server.collaboration.adapter.out.persistence.OrganizeMemberPersistence;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The JPA half of the OrganizeMember reference slice.
 *
 * <p>Bound by the same selector as {@link OrganizeJpaPersistenceAdapter}, never independently. The
 * two aggregates share the owner invariant and the row lock that protects it, so a half-migrated
 * slice could hold the invariant in one store and violate it in the other.
 *
 * <p>{@code countOwnersByOrganizeId} counts in the database rather than loading rows and filtering
 * in memory, matching the MyBatis count query. Loading and filtering would give the same answer for
 * small memberships and quietly change complexity for large ones.
 */
@RequiredArgsConstructor
public class OrganizeMemberJpaPersistenceAdapter implements OrganizeMemberPersistence {

    private final OrganizeMemberJpaRepository organizeMemberJpaRepository;

    @Override
    public OrganizeMember save(OrganizeMember member) {
        try {
            OrganizeMemberJpaEntity saved = organizeMemberJpaRepository.save(new OrganizeMemberJpaEntity(
                    null,
                    member.getOrganizeId() != null ? member.getOrganizeId().getValue() : null,
                    member.getUserId() != null ? member.getUserId().getValue() : null,
                    member.getRole() != null ? member.getRole().name() : null,
                    member.getJoinedAt()));
            return toDomain(saved);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save organize member", e);
        }
    }

    @Override
    public boolean existsByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId) {
        try {
            if (organizeId == null || userId == null) {
                return false;
            }
            return organizeMemberJpaRepository.existsByOrganizeIdAndUserId(
                    organizeId.getValue(), userId.getValue());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during organize member existence check", e);
        }
    }

    @Override
    public Optional<OrganizeMember> findByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId) {
        try {
            if (organizeId == null || userId == null) {
                return Optional.empty();
            }
            return organizeMemberJpaRepository
                    .findByOrganizeIdAndUserId(organizeId.getValue(), userId.getValue())
                    .map(this::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize member", e);
        }
    }

    @Override
    public void deleteByOrganizeIdAndUserId(OrganizeId organizeId, MemberUserId userId) {
        try {
            if (organizeId == null || userId == null) {
                return;
            }
            organizeMemberJpaRepository.deleteByOrganizeIdAndUserId(organizeId.getValue(), userId.getValue());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete organize member", e);
        }
    }

    @Override
    public List<OrganizeMember> findAllByOrganizeId(OrganizeId organizeId) {
        try {
            if (organizeId == null) {
                return List.of();
            }
            return organizeMemberJpaRepository.findAllByOrganizeId(organizeId.getValue())
                    .stream().map(this::toDomain).toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize members", e);
        }
    }

    @Override
    public Optional<OrganizeMemberRole> findRoleByOrganizeIdAndUserId(Long organizeId, Long userId) {
        try {
            if (organizeId == null || userId == null) {
                return Optional.empty();
            }
            return organizeMemberJpaRepository.findByOrganizeIdAndUserId(organizeId, userId)
                    .map(entity -> OrganizeMemberRole.from(entity.getRole()));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize member role", e);
        }
    }

    @Override
    public long countOwnersByOrganizeId(Long organizeId) {
        try {
            if (organizeId == null) {
                return 0L;
            }
            return organizeMemberJpaRepository.countByOrganizeIdAndRole(
                    organizeId, OrganizeMemberRole.OWNER.name());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during count organize owners", e);
        }
    }

    private OrganizeMember toDomain(OrganizeMemberJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrganizeMember.create(
                OrganizeId.of(entity.getOrganizeId()),
                MemberUserId.of(entity.getUserId()),
                OrganizeMemberRole.from(entity.getRole()),
                entity.getJoinedAt());
    }
}
