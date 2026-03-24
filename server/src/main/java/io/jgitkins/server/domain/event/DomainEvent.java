package io.jgitkins.server.domain.event;

import java.time.Instant;

/**
 * Marker for domain events raised by aggregates. Events are immutable value objects
 * that describe something that already happened inside the domain model.
 */
public interface DomainEvent {

    /**
     * Timestamp describing when the event occurred.
     */
    Instant occurredAt();

    /**
     * Event type identifier for logging/dispatching. Defaults to the class simple name.
     */
    default String type() {
        return getClass().getSimpleName();
    }
}
