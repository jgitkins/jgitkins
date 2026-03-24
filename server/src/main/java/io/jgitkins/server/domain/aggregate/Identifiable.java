package io.jgitkins.server.domain.aggregate;

/**
 * Marker interface representing domain objects that expose an immutable identifier.
 * Aggregates and entities can extend this to participate in generic workflows (auditing, locking, etc.).
 *
 * @param <ID> domain-specific identifier type (UUID, value object, etc.)
 */
public interface Identifiable<ID> {

    ID getId();
}
