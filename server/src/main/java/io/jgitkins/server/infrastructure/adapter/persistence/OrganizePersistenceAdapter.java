package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.OrganizeDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeEntity;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeEntityCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrganizePersistenceAdapter implements OrganizePersistencePort {

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
