package io.jgitkins.server.domain.aggregate;

/**
 * Marker interface for aggregate roots. Extends {@link Identifiable} so generic use cases
 * can enforce optimistic locking, auditing, etc., without leaking implementation details.
 */
public interface AggregateRoot<ID> extends Identifiable<ID> {
}
