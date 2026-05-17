package io.jgitkins.server.domain.event;

import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.UserId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrganizeCreatedEvent implements DomainEvent {

    private final OrganizeId organizeId;
    private final OrganizeName name;
    private final UserId ownerId;
    private final Instant occurredAt;

    public static OrganizeCreatedEvent from(Organize organize) {
        return new OrganizeCreatedEvent(
                organize.getId(),
                organize.getName(),
                organize.getOwnerId(),
                Instant.now()
        );
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
