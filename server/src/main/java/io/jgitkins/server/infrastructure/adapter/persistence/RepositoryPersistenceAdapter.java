package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.RepositoryDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.RepositoryEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.UserEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryEntity;
import io.jgitkins.server.infrastructure.persistence.model.RepositoryEntityCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryPersistenceAdapter implements RepositoryPersistencePort {

    private final OrganizeEntityMbgMapper organizeEntityMbgMapper;
    private final RepositoryEntityMbgMapper repositoryEntityMbgMapper;
    private final UserEntityMbgMapper userEntityMbgMapper;

    private final RepositoryDomainMapper repositoryDomainMapper;

    @Override
    public Repository save(Repository repository) {
        try {
            RepositoryEntity entity = repositoryDomainMapper.toEntity(repository);
            LocalDateTime now = LocalDateTime.now();
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            repositoryEntityMbgMapper.insertSelective(entity);
            log.debug("Repository saved. repositoryId={}, ownerType={}, ownerId={}, name={}",
                    entity.getId(),
                    entity.getOwnerType(),
                    entity.getOwnerId(),
                    entity.getName());
            return repository.withIdentity(RepositoryId.of(entity.getId()), entity.getCreatedAt(),
                    entity.getUpdatedAt());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save repository", e);
        }
    }

    @Override
    public Repository update(Repository repository) {
        try {
            RepositoryEntity entity = repositoryDomainMapper.toEntity(repository);
            entity.setUpdatedAt(LocalDateTime.now());
            repositoryEntityMbgMapper.updateByPrimaryKeySelective(entity);
            return repository.withIdentity(repository.getId(), repository.getCreatedAt(), entity.getUpdatedAt());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during update repository", e);
        }
    }

    @Override
    public void deleteById(RepositoryId id) {
        try {
            repositoryEntityMbgMapper.deleteByPrimaryKey(id.getValue());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete repository", e);
        }
    }

    @Override
    public Optional<Repository> findById(RepositoryId id) {
        try {
            RepositoryEntity entity = repositoryEntityMbgMapper.selectByPrimaryKey(id.getValue());
            return Optional.ofNullable(entity).map(repositoryDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository by id", e);
        }
    }

    @Override
    public List<Repository> findAll() {
        try {
            return repositoryEntityMbgMapper.selectByConditionWithBLOBs(new RepositoryEntityCondition())
                    .stream()
                    .map(repositoryDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all repositories", e);
        }
    }

    @Override
    public Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path) {
        try {
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.createCriteria()
                    .andOwnerTypeEqualTo(ownerType.name())
                    .andOwnerIdEqualTo(ownerId.getValue())
                    .andPathEqualTo(path.getValue());
            List<RepositoryEntity> entities = repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition);
            return entities.stream().findFirst().map(repositoryDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository by owner and path", e);
        }
    }

    @Override
    public Optional<Repository> findByClonePath(String clonePath) {
        try {
            if (clonePath == null || clonePath.isBlank()) {
                return Optional.empty();
            }
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.createCriteria().andClonePathEqualTo(clonePath.trim());
            List<RepositoryEntity> entities = repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition);
            return entities.stream().findFirst().map(repositoryDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository by clone path", e);
        }
    }

    @Override
    public Optional<Repository> findByPath(String path) {
        try {
            if (path == null || path.isBlank()) {
                return Optional.empty();
            }
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.createCriteria().andPathEqualTo(path.trim());
            List<RepositoryEntity> entities = repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition);
            return entities.stream().findFirst().map(repositoryDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository by path", e);
        }
    }

    @Override
    public Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name) {
        try {
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.createCriteria()
                    .andOwnerTypeEqualTo(ownerType.name())
                    .andOwnerIdEqualTo(ownerId.getValue())
                    .andNameEqualTo(name.getValue());
            List<RepositoryEntity> entities = repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition);
            return entities.stream().findFirst().map(repositoryDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository by owner and name", e);
        }
    }

    @Override
    public Optional<Long> findIdByOwnerAndName(OwnerType ownerType, OwnerId ownerId, String repoName) {
        try {
            RepositoryEntityCondition repositoryCondition = new RepositoryEntityCondition();
            repositoryCondition.createCriteria()
                    .andOwnerTypeEqualTo(ownerType.name())
                    .andOwnerIdEqualTo(ownerId.getValue())
                    .andNameEqualTo(repoName);

            List<RepositoryEntity> repositories = repositoryEntityMbgMapper.selectByCondition(repositoryCondition);
            return repositories.stream()
                    .findFirst()
                    .map(RepositoryEntity::getId);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find repository id", e);
        }
    }

    @Override
    public List<Repository> findAllByOwner(OwnerType ownerType, OwnerId ownerId) {
        try {
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.createCriteria()
                    .andOwnerTypeEqualTo(ownerType.name())
                    .andOwnerIdEqualTo(ownerId.getValue());
            return repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition)
                    .stream()
                    .map(repositoryDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all repositories by owner", e);
        }
    }

    @Override
    public long countByOwner(OwnerType ownerType, OwnerId ownerId) {
        try {
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.createCriteria()
                    .andOwnerTypeEqualTo(ownerType.name())
                    .andOwnerIdEqualTo(ownerId.getValue());
            return repositoryEntityMbgMapper.countByCondition(condition);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during count repositories by owner", e);
        }
    }
}
