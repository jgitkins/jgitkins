package io.jgitkins.server.collaboration.adapter.outbound.event;

import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class CollaborationSpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<DomainEvent> snapshot = List.copyOf(events);
        Runnable publishEvents = () -> snapshot.forEach(applicationEventPublisher::publishEvent);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishEvents.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishEvents.run();
            }
        });
    }
}
