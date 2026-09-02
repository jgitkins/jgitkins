package io.jgitkins.server.repository.adapter.out.persistence;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.adapter.out.persistence.support.RepositoryDomainMapper;
import io.jgitkins.server.repository.adapter.out.persistence.translator.RepositoryEntityMbgMapper;
import io.jgitkins.server.repository.adapter.out.persistence.model.RepositoryEntity;
import io.jgitkins.server.repository.adapter.out.persistence.model.RepositoryEntityCondition;
import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.service.internal.CloneUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registered by {@code RepositoryPersistenceSelectorConfiguration}, not by component scanning.
 *
 * <p>The {@code @Component} annotation was removed in task 2.72: with a JPA implementation of the
 * same port on the classpath, scanning would register two candidates and the injection point would
 * be ambiguous. The composition root now names exactly one.
 */
@RequiredArgsConstructor
@Slf4j
public class RepositoryPersistenceAdapter implements RepositoryPersistence {

    // USER and ORGANIZE_MEMBER belong to identity and collaboration, so this adapter asks through
    // ports instead of importing their mappers and entities. See CrossContextPersistenceCouplingArchitectureTest.
    private final UserNamespacePort userNamespacePort;
    private final OrganizationNamespacePort organizationNamespacePort;
    private final OrganizationMembershipPort organizationMembershipPort;
    private final RepositoryEntityMbgMapper repositoryEntityMbgMapper;

    private final CloneUrlBuilder cloneUrlBuilder;
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
    public Optional<Repository> findByOwnerAndPath(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryPath path) {
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
    public Optional<Repository> findByOwnerAndName(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryName name) {
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
    public Optional<RepositoryResult> loadRepository(Long repositoryId) {
        try {
            return Optional.ofNullable(repositoryEntityMbgMapper.selectByPrimaryKey(repositoryId))
                    .map(this::toResult);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during load repository", e);
        }
    }

    @Override
    public Optional<RepositoryResult> loadRepositoryByPath(String namespace, String repoName) {
        try {
            String normalizedNamespace = trimSlashes(namespace);
            String normalizedRepoName = trimSlashes(repoName);

            return findEntityByClonePath(buildClonePath(normalizedNamespace, normalizedRepoName))
                    .or(() -> findUserOwnedEntity(normalizedNamespace, normalizedRepoName))
                    .or(() -> findOrganizationOwnedEntity(normalizedNamespace, normalizedRepoName))
                    .map(this::toResult);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during load repository by path", e);
        }
    }

    @Override
    public List<RepositoryResult> loadVisibleRepositories(Long requesterId) {
        // Before the try, so the catch below cannot relabel a collaboration failure. Safe to call
        // unconditionally: the port answers empty for a null requester.
        List<Long> organizeIds = findOrganizationIdsByUserId(requesterId);
        try {
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.setDistinct(true);
            condition.setOrderByClause("UPDATED_AT desc");

            condition.or().andVisibilityEqualTo("PUBLIC");

            if (requesterId != null) {
                condition.or()
                        .andOwnerTypeEqualTo(OwnerType.USER.name())
                        .andOwnerIdEqualTo(requesterId);

                if (!organizeIds.isEmpty()) {
                    condition.or()
                            .andOwnerTypeEqualTo(OwnerType.ORGANIZATION.name())
                            .andOwnerIdIn(organizeIds);
                }
            }

            return repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition)
                    .stream()
                    .map(this::toResult)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during load visible repositories", e);
        }
    }

    @Override
    public List<RepositoryResult> loadUserRepositories(String username, Long requesterId) {
        try {
            Optional<Long> owner = findUserIdByUsername(username);
            if (owner.isEmpty()) {
                return List.of();
            }

            Long ownerId = owner.get();
            RepositoryEntityCondition condition = new RepositoryEntityCondition();
            condition.setOrderByClause("UPDATED_AT desc");
            RepositoryEntityCondition.Criteria criteria = condition.createCriteria()
                    .andOwnerTypeEqualTo(OwnerType.USER.name())
                    .andOwnerIdEqualTo(ownerId);

            if (requesterId == null || !requesterId.equals(ownerId)) {
                criteria.andVisibilityEqualTo("PUBLIC");
            }

            return repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition)
                    .stream()
                    .map(this::toResult)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during load user repositories", e);
        }
    }

    @Override
    public long countByOwner(OwnerType ownerType, RepositoryOwnerId ownerId) {
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

    private Optional<RepositoryEntity> findEntityByClonePath(String clonePath) {
        if (clonePath == null || clonePath.isBlank()) {
            return Optional.empty();
        }
        RepositoryEntityCondition condition = new RepositoryEntityCondition();
        condition.createCriteria().andClonePathEqualTo(clonePath.trim());
        return repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition).stream().findFirst();
    }

    private Optional<RepositoryEntity> findUserOwnedEntity(String namespace, String repoName) {
        return findUserIdByUsername(namespace)
                .flatMap(userId -> {
                    RepositoryEntityCondition condition = new RepositoryEntityCondition();
                    condition.createCriteria()
                            .andOwnerTypeEqualTo(OwnerType.USER.name())
                            .andOwnerIdEqualTo(userId)
                            .andNameEqualTo(repoName);
                    return repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition).stream().findFirst();
                });
    }

    private Optional<RepositoryEntity> findOrganizationOwnedEntity(String namespace, String repoName) {
        return findOrganizationIdByName(namespace)
                .flatMap(organizationId -> {
                    RepositoryEntityCondition condition = new RepositoryEntityCondition();
                    condition.createCriteria()
                            .andOwnerTypeEqualTo(OwnerType.ORGANIZATION.name())
                            .andOwnerIdEqualTo(organizationId)
                            .andPathEqualTo(repoName);
                    return repositoryEntityMbgMapper.selectByConditionWithBLOBs(condition).stream().findFirst();
                });
    }

    // The blank guards stay here rather than moving into the ports. Both port implementations guard
    // blank input themselves, so this is belt and braces -- but it also keeps the trim, and trimming
    // before the lookup is behaviour this adapter had.
    private Optional<Long> findUserIdByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userNamespacePort.findUserIdByUsername(username.trim());
    }

    private Optional<Long> findOrganizationIdByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return organizationNamespacePort.findOrganizationIdByName(name.trim());
    }

    /**
     * Outside the caller's try block on purpose.
     *
     * <p>Its only caller invokes it before opening its try block. {@code loadVisibleRepositories}
     * relabels anything that escapes as "failed during load visible repositories", which was accurate
     * while every query in it was this adapter's own. A failure in here is collaboration's, and
     * rewrapping would put a repository-shaped message in front of a collaboration-shaped cause. The
     * port's own InfrastructureException already names what failed.
     */
    private List<Long> findOrganizationIdsByUserId(Long requesterId) {
        return organizationMembershipPort.findOrganizationIdsByUserId(requesterId);
    }

    private RepositoryResult toResult(RepositoryEntity entity) {
        return new RepositoryResult(
                entity.getId(),
                entity.getOwnerType(),
                entity.getName(),
                entity.getPath(),
                entity.getDefaultBranch(),
                entity.getVisibility(),
                entity.getDescription(),
                entity.getOwnerId(),
                entity.getCredentialId(),
                entity.getClonePath(),
                cloneUrlBuilder.build(entity.getClonePath()),
                entity.getLastSyncedAt() == null,
                entity.getLastSyncedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String trimSlashes(String value) {
        if (value == null) {
            throw new IllegalArgumentException("path segment must not be null");
        }
        return value.trim().replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String buildClonePath(String namespace, String repoName) {
        List<String> segments = new ArrayList<>();
        if (!namespace.isBlank()) {
            segments.add(namespace);
        }
        if (!repoName.isBlank()) {
            segments.add(repoName);
        }
        return "/" + String.join("/", segments) + ".git";
    }
}
