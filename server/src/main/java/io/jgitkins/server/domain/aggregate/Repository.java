package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.event.RepositoryProvisionedEvent;
import io.jgitkins.server.domain.event.RepositorySynchronizedEvent;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Repository Aggregate Root
 */
@Getter
public class Repository extends AbstractAggregateRoot<RepositoryId> {

    private final RepositoryId id;
    private final OwnerType ownerType;
    private final OwnerId ownerId;
    private final RepositoryName name;
    private final RepositoryPath path;
    private final BranchName defaultBranch;
    private final RepositoryVisibility visibility;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String credentialId;
    private final String clonePath;
    private final LocalDateTime lastSyncedAt;
    private final boolean requiresInitialContent;
    private final boolean initialized;

    private Repository(RepositoryId id,
                       OwnerType ownerType,
                       OwnerId ownerId,
                       RepositoryName name,
                       RepositoryPath path,
                       BranchName defaultBranch,
                       RepositoryVisibility visibility,
                       String description,
                       LocalDateTime createdAt,
                       LocalDateTime updatedAt,
                       String credentialId,
                       String clonePath,
                       LocalDateTime lastSyncedAt,
                       boolean requiresInitialContent,
                       boolean initialized) {
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.name = name;
        this.path = path;
        this.defaultBranch = defaultBranch;
        this.visibility = visibility;
        this.description = description != null ? description.trim() : null;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.credentialId = credentialId;
        this.clonePath = clonePath;
        this.lastSyncedAt = lastSyncedAt;
        this.requiresInitialContent = requiresInitialContent;
        this.initialized = initialized;
    }

    public static Repository create(OwnerType ownerType,
                                    OwnerId ownerId,
                                    RepositoryName name,
                                    RepositoryPath path,
                                    BranchName defaultBranch,
                                    RepositoryVisibility visibility,
                                    String description,
                                    String clonePath,
                                    String credentialId,
                                    InitialCommitOptions initialCommitOptions) {
        if (initialCommitOptions == null) {
            throw new IllegalArgumentException("InitialCommitOptions must not be null");
        }
        LocalDateTime now = LocalDateTime.now();
        Repository repository = new Repository(
                null,
                ownerType,
                ownerId,
                name,
                path,
                defaultBranch,
                visibility,
                description,
                now,
                now,
                credentialId,
                clonePath,
                null,
                initialCommitOptions.requiresInitialContent(),
                false
        );
        repository.registerEvent(RepositoryProvisionedEvent.from(repository, initialCommitOptions));
        return repository;
    }

    public Repository withIdentity(RepositoryId repositoryId,
                                   LocalDateTime createdAt,
                                   LocalDateTime updatedAt) {
        Repository identified = new Repository(repositoryId,
                                               ownerType,
                                               ownerId,
                                               name,
                                               path,
                                               defaultBranch,
                                               visibility,
                                               description,
                                               createdAt,
                                               updatedAt,
                                               credentialId,
                                               clonePath,
                                               lastSyncedAt,
                                               requiresInitialContent,
                                               initialized);
        identified.copyDomainEventsFrom(this);
        return identified;
    }

    public Repository markInit(LocalDateTime syncedAt) {
        LocalDateTime effectiveSyncedAt = syncedAt != null ? syncedAt : LocalDateTime.now();
        Repository marked = new Repository(id,
                                           ownerType,
                                           ownerId,
                                           name,
                                           path,
                                           defaultBranch,
                                           visibility,
                                           description,
                                           createdAt,
                                           effectiveSyncedAt,
                                           credentialId,
                                           clonePath,
                                           effectiveSyncedAt,
                                           false,
                                           true
        );
        marked.copyDomainEventsFrom(this);
        marked.registerEvent(RepositorySynchronizedEvent.from(marked));
        return marked;
    }

    // internal factory method (entity to domain)
    public static Repository rehydrate(RepositoryId repositoryId,
                                       OwnerType ownerType,
                                       OwnerId ownerId,
                                       RepositoryName name,
                                       RepositoryPath path,
                                       BranchName defaultBranch,
                                       RepositoryVisibility visibility,
                                       String description,
                                       String clonePath,
                                       String credentialId,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt,
                                       LocalDateTime lastSyncedAt) {
        return new Repository(repositoryId,
                              ownerType,
                              ownerId,
                              name,
                              path,
                              defaultBranch,
                              visibility,
                              description,
                              createdAt,
                              updatedAt,
                              credentialId,
                              clonePath,
                              lastSyncedAt,
                              lastSyncedAt == null,
                              lastSyncedAt != null);
    }

    public OrganizeId getOrganizeId() {
        if (ownerType == OwnerType.ORGANIZATION && ownerId != null) {
            return OrganizeId.of(ownerId.getValue());
        }
        return null;
    }
}
