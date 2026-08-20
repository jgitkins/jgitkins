package io.jgitkins.server.collaboration.adapter.out.event;

import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class CollaborationSpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CollaborationSpringDomainEventPublisher.class);
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        List<DomainEvent> snapshot = List.copyOf(events);
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Domain events require an active transaction");
        }

        Runnable publishEvents = () -> snapshot.forEach(applicationEventPublisher::publishEvent);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    publishEvents.run();
                } catch (RuntimeException exception) {
                    log.error("Collaboration domain event delivery failed after transaction commit", exception);
                }
            }
        });
    }
}
