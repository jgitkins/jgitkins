package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.repository.adapter.out.persistence.RepositoryPersistence;
import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.service.internal.CloneUrlBuilder;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA implementation of the repository-context persistence pair.
 *
 * <p>The three cross-context reads — resolve a username, resolve an organization name, list a user's
 * organization ids — go through the collaboration and identity JPA repositories directly. That is the
 * coupling the MyBatis adapter already has: it injects {@code OrganizeEntityMbgMapper},
 * {@code OrganizeMemberEntityMbgMapper} and {@code UserEntityMbgMapper} from those same contexts.
 * Reproducing it keeps this task a technology swap; turning those reads into port calls is a boundary
 * change, and boundary changes belong to task 2.67, which owns the final placement rule. The context
 * guard permits it: {@code RepositoryBoundedContextArchitectureTest} forbids foreign imports under
 * {@code repository/domain}, not under the outbound adapter.
 *
 * <p>{@code save} inserts and never updates, matching the MyBatis adapter, which called
 * {@code insertSelective} unconditionally. {@code update} is the separate port method.
 */
@RequiredArgsConstructor
@Slf4j
public class RepositoryJpaPersistenceAdapter implements RepositoryPersistence {

    private static final String PUBLIC_VISIBILITY = "PUBLIC";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REGISTERED = "REGISTERED";

    private final RepositoryJpaRepository repositoryJpaRepository;
    // Ports, not the other contexts' JPA repositories: naming OrganizeJpaRepository bound this to
    // collaboration's table shape and to collaboration being on JPA. Same three ports as the MyBatis sibling.
    private final UserNamespacePort userNamespacePort;
    private final OrganizationNamespacePort organizationNamespacePort;
    private final OrganizationMembershipPort organizationMembershipPort;
    private final CloneUrlBuilder cloneUrlBuilder;

    @Override
    public Repository save(Repository repository) {
        try {
            RepositoryJpaEntity entity = toEntity(repository);
            LocalDateTime now = LocalDateTime.now();
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
            if (entity.getUpdatedAt() == null) {
                entity.setUpdatedAt(entity.getCreatedAt());
            }
            RepositoryJpaEntity saved = repositoryJpaRepository.save(entity);
            log.debug("Repository saved. repositoryId={}, ownerType={}, ownerId={}, name={}",
                    saved.getId(), saved.getOwnerType(), saved.getOwnerId(), saved.getName());
            return repository.withIdentity(RepositoryId.of(saved.getId()), saved.getCreatedAt(),
                    saved.getUpdatedAt());
        } catch (Exception e) {
            throw persistence("Database operation failed during save repository", e);
        }
    }

    @Override
    public Repository update(Repository repository) {
        try {
            RepositoryJpaEntity entity = toEntity(repository);
            LocalDateTime updatedAt = LocalDateTime.now();
            entity.setUpdatedAt(updatedAt);
            repositoryJpaRepository.save(entity);
            return repository.withIdentity(repository.getId(), repository.getCreatedAt(), updatedAt);
        } catch (Exception e) {
            throw persistence("Database operation failed during update repository", e);
        }
    }

    @Override
    public void deleteById(RepositoryId id) {
        try {
            repositoryJpaRepository.deleteById(id.getValue());
        } catch (Exception e) {
            throw persistence("Database operation failed during delete repository", e);
        }
    }

    @Override
    public Optional<Repository> findById(RepositoryId id) {
        try {
            return repositoryJpaRepository.findById(id.getValue()).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find repository by id", e);
        }
    }

    @Override
    public Optional<Repository> findByOwnerAndPath(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryPath path) {
        try {
            return repositoryJpaRepository
                    .findFirstByOwnerTypeAndOwnerIdAndPath(ownerType.name(), ownerId.getValue(), path.getValue())
                    .map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find repository by owner and path", e);
        }
    }

    @Override
    public Optional<Repository> findByClonePath(String clonePath) {
        try {
            if (clonePath == null || clonePath.isBlank()) {
                return Optional.empty();
            }
            return repositoryJpaRepository.findFirstByClonePath(clonePath.trim()).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find repository by clone path", e);
        }
    }

    @Override
    public Optional<Repository> findByPath(String path) {
        try {
            if (path == null || path.isBlank()) {
                return Optional.empty();
            }
            return repositoryJpaRepository.findFirstByPath(path.trim()).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find repository by path", e);
        }
    }

    @Override
    public Optional<Repository> findByOwnerAndName(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryName name) {
        try {
            return repositoryJpaRepository
                    .findFirstByOwnerTypeAndOwnerIdAndName(ownerType.name(), ownerId.getValue(), name.getValue())
                    .map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find repository by owner and name", e);
        }
    }

    @Override
    public Optional<RepositoryResult> loadRepository(Long repositoryId) {
        try {
            if (repositoryId == null) {
                return Optional.empty();
            }
            return repositoryJpaRepository.findById(repositoryId).map(this::toResult);
        } catch (Exception e) {
            throw persistence("Database operation failed during load repository", e);
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
            throw persistence("Database operation failed during load repository by path", e);
        }
    }

    @Override
    public List<RepositoryResult> loadVisibleRepositories(Long requesterId) {
        // Before the try, so the catch below cannot relabel a collaboration failure as this adapter's.
        // Safe to call unconditionally: the port answers empty for a null requester.
        List<Long> organizeIds = findOrganizationIdsByUserId(requesterId);
        try {
            List<RepositoryJpaEntity> entities;
            if (requesterId == null) {
                entities = repositoryJpaRepository.findVisibleToAnonymous(PUBLIC_VISIBILITY);
            } else {
                entities = organizeIds.isEmpty()
                        ? repositoryJpaRepository.findVisibleToUser(
                                PUBLIC_VISIBILITY, OwnerType.USER.name(), requesterId)
                        : repositoryJpaRepository.findVisibleToUserInOrganizations(
                                PUBLIC_VISIBILITY, OwnerType.USER.name(), requesterId,
                                OwnerType.ORGANIZATION.name(), organizeIds);
            }
            return entities.stream().map(this::toResult).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during load visible repositories", e);
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
            boolean ownViewing = requesterId != null && requesterId.equals(ownerId);
            List<RepositoryJpaEntity> entities = ownViewing
                    ? repositoryJpaRepository.findAllByOwnerTypeAndOwnerIdOrderByUpdatedAtDesc(
                            OwnerType.USER.name(), ownerId)
                    : repositoryJpaRepository.findAllByOwnerTypeAndOwnerIdAndVisibilityOrderByUpdatedAtDesc(
                            OwnerType.USER.name(), ownerId, PUBLIC_VISIBILITY);
            return entities.stream().map(this::toResult).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during load user repositories", e);
        }
    }

    @Override
    public long countByOwner(OwnerType ownerType, RepositoryOwnerId ownerId) {
        try {
            return repositoryJpaRepository.countByOwnerTypeAndOwnerId(ownerType.name(), ownerId.getValue());
        } catch (Exception e) {
            throw persistence("Database operation failed during count repositories by owner", e);
        }
    }

    private Optional<RepositoryJpaEntity> findEntityByClonePath(String clonePath) {
        if (clonePath == null || clonePath.isBlank()) {
            return Optional.empty();
        }
        return repositoryJpaRepository.findFirstByClonePath(clonePath.trim());
    }

    private Optional<RepositoryJpaEntity> findUserOwnedEntity(String namespace, String repoName) {
        return findUserIdByUsername(namespace)
                .flatMap(userId -> repositoryJpaRepository.findFirstByOwnerTypeAndOwnerIdAndName(
                        OwnerType.USER.name(), userId, repoName));
    }

    private Optional<RepositoryJpaEntity> findOrganizationOwnedEntity(String namespace, String repoName) {
        return findOrganizationIdByName(namespace)
                .flatMap(organizationId -> repositoryJpaRepository.findFirstByOwnerTypeAndOwnerIdAndPath(
                        OwnerType.ORGANIZATION.name(), organizationId, repoName));
    }

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

    private List<Long> findOrganizationIdsByUserId(Long requesterId) {
        return organizationMembershipPort.findOrganizationIdsByUserId(requesterId);
    }

    private RepositoryJpaEntity toEntity(Repository repository) {
        RepositoryJpaEntity entity = new RepositoryJpaEntity();
        entity.setId(repository.getId() != null ? repository.getId().getValue() : null);
        entity.setName(repository.getName().getValue());
        entity.setPath(repository.getPath().getValue());
        entity.setOwnerType(repository.getOwnerType() != null ? repository.getOwnerType().name() : null);
        entity.setOwnerId(repository.getOwnerId() != null ? repository.getOwnerId().getValue() : null);
        entity.setCredentialId(repository.getCredentialId());
        entity.setClonePath(repository.getClonePath());
        entity.setDescription(repository.getDescription());
        entity.setDefaultBranch(repository.getDefaultBranch().getValue());
        entity.setVisibility(repository.getVisibility().name());
        // Same derivation the MyBatis mapper applied on every write; STATUS is app-owned.
        entity.setStatus(repository.getLastSyncedAt() != null ? STATUS_ACTIVE : STATUS_REGISTERED);
        entity.setLastSyncedAt(repository.getLastSyncedAt());
        entity.setCreatedAt(repository.getCreatedAt());
        entity.setUpdatedAt(repository.getUpdatedAt());
        return entity;
    }

    private Repository toDomain(RepositoryJpaEntity entity) {
        return Repository.rehydrate(
                RepositoryId.of(entity.getId()),
                OwnerType.from(entity.getOwnerType()),
                entity.getOwnerId() != null
                        ? RepositoryOwnerId.fromStoredValue(
                                entity.getOwnerId(), "REPOSITORY id=" + entity.getId())
                        : null,
                RepositoryName.from(entity.getName()),
                RepositoryPath.from(entity.getPath()),
                BranchName.of(entity.getDefaultBranch()),
                RepositoryVisibility.from(entity.getVisibility()),
                entity.getDescription(),
                entity.getClonePath(),
                entity.getCredentialId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastSyncedAt());
    }

    private RepositoryResult toResult(RepositoryJpaEntity entity) {
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
                entity.getUpdatedAt());
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

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
