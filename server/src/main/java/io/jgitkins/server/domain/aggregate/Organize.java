package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.UserId;
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
    private final UserId ownerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Organize create(OrganizeName name,
                                  UserId ownerId,
                                  String description) {
        LocalDateTime now = LocalDateTime.now();
        Organize organize = new Organize(null,
                                         name,
                                         normalizeDescription(description),
                                         ownerId,
                                         now,
                                         now);

        organize.registerEvent(OrganizeCreatedEvent.from(organize));
        return organize;
    }

    public static Organize reconstruct(OrganizeId id,
                                       OrganizeName name,
                                       String description,
                                       UserId ownerId,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        return new Organize(id, name, description, ownerId, createdAt, updatedAt);
    }

    private static String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}
