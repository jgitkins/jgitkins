package io.jgitkins.server.collaboration.adapter.out.event;

import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes collaboration domain events onto the application event bus, but only once the
 * surrounding transaction has committed.
 *
 * <p>Registration time and delivery time are different moments, which is the one thing about
 * this class that is not obvious from reading it top to bottom:
 *
 * <pre>
 * publish(events)                        afterCommit()   [runs only if the tx committed]
 *   |                                      |
 *   +- no active tx?      --&gt; throw        +- deliver event[0] -- throws --&gt; log, keep going
 *   +- null/empty?        --&gt; return       +- deliver event[1] -- throws --&gt; log, keep going
 *   +- snapshot = copyOf(events)           +- deliver event[n] ...
 *   +- register synchronization ---------&gt; on rollback this block never runs
 * </pre>
 *
 * <p>The transaction check runs before the empty check on purpose: the contract is "callers must
 * hold a transaction", and letting an empty list slip past the guard would make that contract
 * depend on the data rather than on the call site.
 *
 * <p>Delivery is isolated per event. One failing listener must not swallow the events queued
 * behind it, and the log carries the event type so a post-mortem can tell what was lost. The
 * commit already happened, so a delivery failure is not recoverable here by design.
 *
 * <p>Each call registers its own synchronization. Do not call this in a loop inside one
 * transaction; see the deferred-deletion note in TODOS.md.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationSpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(List<DomainEvent> events) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Domain events require an active transaction");
        }

        if (events == null || events.isEmpty()) {
            return;
        }

        List<DomainEvent> snapshot = List.copyOf(events);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                snapshot.forEach(CollaborationSpringDomainEventPublisher.this::deliver);
            }
        });
    }

    private void deliver(DomainEvent event) {
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.error("Collaboration domain event delivery failed after transaction commit: {}",
                    event.type(), exception);
        }
    }
}
