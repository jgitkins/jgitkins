package io.jgitkins.server.collaboration.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OrganizeTest {

    @Test
    void create_usesApplicationProvidedIdentityAndTime() {
        OrganizeId id = OrganizeId.of(10L);
        OrganizeName name = OrganizeName.from("core-team");
        OrganizeOwnerId ownerId = OrganizeOwnerId.of(7L);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 14, 9, 0);

        Organize organize = Organize.create(
                id,
                name,
                ownerId,
                "Core Team",
                createdAt,
                Instant.parse("2026-08-14T00:00:00Z")
        );

        assertThat(organize.getId()).isEqualTo(id);
        assertThat(organize.getName()).isEqualTo(name);
        assertThat(organize.getOwnerId()).isEqualTo(ownerId);
        assertThat(organize.getCreatedAt()).isEqualTo(createdAt);
        assertThat(organize.getUpdatedAt()).isEqualTo(createdAt);
    }

    @Test
    void create_registersEventWithApplicationProvidedOccurrenceTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 14, 9, 0);

        Instant occurredAt = Instant.parse("2026-08-14T00:00:00Z");

        Organize organize = Organize.create(
                OrganizeId.of(10L),
                OrganizeName.from("core-team"),
                OrganizeOwnerId.of(7L),
                "Core Team",
                LocalDateTime.of(2026, 8, 14, 9, 0),
                occurredAt
        );

        OrganizeCreatedEvent event = (OrganizeCreatedEvent) organize.getDomainEvents().get(0);

        assertThat(event.getOrganizeId()).isEqualTo(OrganizeId.of(10L));
        assertThat(event.getOwnerId()).isEqualTo(OrganizeOwnerId.of(7L));
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
    }
}
