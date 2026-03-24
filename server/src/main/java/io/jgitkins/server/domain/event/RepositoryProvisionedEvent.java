package io.jgitkins.server.domain.event;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RepositoryProvisionedEvent implements DomainEvent {

    private final RepositoryId repositoryId;
    private final OwnerType ownerType;
    private final OwnerId ownerId;
    private final RepositoryName name;
    private final RepositoryPath path;
    private final BranchName defaultBranch;
    private final RepositoryVisibility visibility;
    private final InitialCommitOptions initialCommitOptions;
    private final Instant occurredAt;

    public static RepositoryProvisionedEvent from(Repository repository,
                                                  InitialCommitOptions initialCommitOptions) {
        return new RepositoryProvisionedEvent(
                repository.getId(),
                repository.getOwnerType(),
                repository.getOwnerId(),
                repository.getName(),
                repository.getPath(),
                repository.getDefaultBranch(),
                repository.getVisibility(),
                Objects.requireNonNull(initialCommitOptions, "initialCommitOptions must not be null"),
                Instant.now()
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
