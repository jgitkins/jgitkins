package io.jgitkins.server.collaboration.domain.aggregate;

import io.jgitkins.server.collaboration.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.shared.domain.aggregate.AbstractAggregateRoot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Organize Aggregate Root
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Organize extends AbstractAggregateRoot<OrganizeId> {

    private final OrganizeId id;
    private final OrganizeName name;
    private final String description;
    private final OwnerId ownerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Organize create(OrganizeId id,
                                  OrganizeName name,
                                  OwnerId ownerId,
                                  String description,
                                  LocalDateTime createdAt,
                                  java.time.Instant occurredAt) {
        Organize organize = new Organize(id,
                                         name,
                                         normalizeDescription(description),
                                         ownerId,
                                         createdAt,
                                         createdAt);

        organize.registerEvent(OrganizeCreatedEvent.from(organize, occurredAt));
        return organize;
    }

    public static Organize createWithoutEvent(OrganizeId id,
                                               OrganizeName name,
                                               OwnerId ownerId,
                                               String description,
                                               LocalDateTime createdAt) {
        return new Organize(id,
                name,
                normalizeDescription(description),
                ownerId,
                createdAt,
                createdAt);
    }

    public void recordCreated(java.time.Instant occurredAt) {
        if (id == null) {
            throw new IllegalStateException("Organize must be persisted before recording creation event");
        }
        registerEvent(OrganizeCreatedEvent.from(this, occurredAt));
    }
    public static Organize reconstruct(OrganizeId id,
                                       OrganizeName name,
                                       String description,
                                       OwnerId ownerId,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        return new Organize(id, name, description, ownerId, createdAt, updatedAt);
    }

    private static String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}
