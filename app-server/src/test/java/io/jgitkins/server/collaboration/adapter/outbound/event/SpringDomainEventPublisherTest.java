package io.jgitkins.server.collaboration.adapter.outbound.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CollaborationSpringDomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void publish_failsFastWhenNoTransactionIsActive() {
        CollaborationSpringDomainEventPublisher publisher = new CollaborationSpringDomainEventPublisher(applicationEventPublisher);
        Organize organize = Organize.create(
                OrganizeId.of(10L),
                OrganizeName.from("core-team"),
                OwnerId.of(7L),
                "Core Team",
                java.time.LocalDateTime.of(2026, 8, 14, 9, 0),
                Instant.parse("2026-08-14T00:00:00Z"));
        DomainEvent event = organize.getDomainEvents().get(0);

        assertThrows(IllegalStateException.class, () -> publisher.publish(List.of(event)));

        verify(applicationEventPublisher, never()).publishEvent(event);
    }
}
