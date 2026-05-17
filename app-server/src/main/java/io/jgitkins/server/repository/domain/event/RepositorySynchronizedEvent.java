package io.jgitkins.server.repository.domain.event;

import io.jgitkins.server.domain.event.DomainEvent;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RepositorySynchronizedEvent implements DomainEvent {

    private final RepositoryId repositoryId;
    private final RepositoryName name;
    private final RepositoryPath path;
    private final LocalDateTime syncedAt;
    private final Instant occurredAt;

    public static RepositorySynchronizedEvent from(Repository repository) {
        return new RepositorySynchronizedEvent(
                repository.getId(),
                repository.getName(),
                repository.getPath(),
                repository.getLastSyncedAt(),
                Instant.now()
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
