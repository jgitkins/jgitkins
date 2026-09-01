package io.jgitkins.server.collaboration.domain.event;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrganizeCreatedEvent implements DomainEvent {

    private final OrganizeId organizeId;
    private final OrganizeName name;
    private final OrganizeOwnerId ownerId;
    private final Instant occurredAt;

    public static OrganizeCreatedEvent from(Organize organize, Instant occurredAt) {
        return new OrganizeCreatedEvent(
                organize.getId(),
                organize.getName(),
                organize.getOwnerId(),
                occurredAt
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
