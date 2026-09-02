package io.jgitkins.server.collaboration.adapter.out.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OrganizeOwnerId;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
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

    private static DomainEvent anEvent() {
        return Organize.create(
                        OrganizeId.of(10L),
                        OrganizeName.from("core-team"),
                        OrganizeOwnerId.of(7L),
                        "Core Team",
                        LocalDateTime.of(2026, 8, 14, 9, 0),
                        Instant.parse("2026-08-14T00:00:00Z"))
                .getDomainEvents()
                .get(0);
    }

    @Test
    void publish_failsFastWhenNoTransactionIsActive() {
        CollaborationSpringDomainEventPublisher publisher =
                new CollaborationSpringDomainEventPublisher(applicationEventPublisher);
        DomainEvent event = anEvent();

        assertThrows(IllegalStateException.class, () -> publisher.publish(List.of(event)));

        verify(applicationEventPublisher, never()).publishEvent(event);
    }

    /**
     * The guard must not depend on the payload. An empty list used to return before the
     * transaction check, so the same call site honoured the contract or skipped it depending on
     * whether the aggregate happened to have raised anything.
     */
    @Test
    void publish_failsFastForAnEmptyListWhenNoTransactionIsActive() {
        CollaborationSpringDomainEventPublisher publisher =
                new CollaborationSpringDomainEventPublisher(applicationEventPublisher);

        assertThrows(IllegalStateException.class, () -> publisher.publish(List.of()));

        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void publish_failsFastForNullWhenNoTransactionIsActive() {
        CollaborationSpringDomainEventPublisher publisher =
                new CollaborationSpringDomainEventPublisher(applicationEventPublisher);

        assertThrows(IllegalStateException.class, () -> publisher.publish(null));

        verifyNoInteractions(applicationEventPublisher);
    }
}
