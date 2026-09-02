package io.jgitkins.server.collaboration.adapter.out.persistence;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.collaboration.adapter.out.persistence.support.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.translator.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.adapter.out.persistence.model.OrganizeEntity;
import io.jgitkins.server.collaboration.adapter.out.persistence.model.OrganizeEntityCondition;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class OrganizePersistenceAdapter implements OrganizePersistence {

    private final OrganizeEntityMbgMapper organizeEntityMbgMapper;

    private final OrganizeDomainMapper organizeDomainMapper;

    @Override
    public Organize save(Organize organize) {
        try {
            OrganizeEntity entity = organizeDomainMapper.toEntity(organize);
            organizeEntityMbgMapper.insertSelective(entity);
            return organizeDomainMapper.toDomain(entity);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save organize", e);
        }
    }

    @Override
    public Organize update(Organize organize) {
        try {
            OrganizeEntity entity = organizeDomainMapper.toEntity(organize);
            organizeEntityMbgMapper.updateByPrimaryKeySelective(entity);
            return organizeDomainMapper.toDomain(entity);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during update organize", e);
        }
    }

    @Override
    public Optional<Organize> findById(OrganizeId organizeId) {
        try {
            if (organizeId == null) {
                return Optional.empty();
            }
            OrganizeEntity entity = organizeEntityMbgMapper.selectByPrimaryKey(organizeId.getValue());
            return Optional.ofNullable(organizeDomainMapper.toDomain(entity));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize by id", e);
        }
    }

    @Override
    public Organize lockByIdForMembershipMutation(OrganizeId organizeId) {
        try {
            OrganizeEntity entity = organizeEntityMbgMapper.selectByOrganizeIdForUpdate(organizeId.getValue());
            if (entity == null) {
                throw new io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException(organizeId.getValue());
            }
            return organizeDomainMapper.toDomain(entity);
        } catch (io.jgitkins.server.collaboration.application.exception.OrganizeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during lock organize for membership mutation", e);
        }
    }

    @Override
    public Optional<Organize> findByName(OrganizeName name) {
        try {
            OrganizeEntityCondition condition = new OrganizeEntityCondition();
            condition.createCriteria().andNameEqualTo(name.getValue());
            List<OrganizeEntity> entities = organizeEntityMbgMapper.selectByCondition(condition);
            return entities.stream().findFirst().map(organizeDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find organize by name", e);
        }
    }

    @Override
    public List<Organize> findAll() {
        try {
            OrganizeEntityCondition condition = new OrganizeEntityCondition();
            List<OrganizeEntity> entities = organizeEntityMbgMapper.selectByCondition(condition);
            return entities.stream().map(organizeDomainMapper::toDomain).toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all organizes", e);
        }
    }

    @Override
    public void deleteById(OrganizeId organizeId) {
        try {
            organizeEntityMbgMapper.deleteByPrimaryKey(organizeId.getValue());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete organize", e);
        }
    }
}
