package io.jgitkins.server.collaboration.adapter.out.persistence;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;

/**
 * The two OrganizeMember outbound ports, always served by one object.
 *
 * <p>Same reason as {@link OrganizePersistence}: it gives the selector one type to switch on, and it
 * records that membership writes and membership queries have to come from the same implementation.
 * The owner invariant is enforced by reading the membership count inside the same transaction that
 * mutates it, so splitting the two ports across providers would break it.
 */
public interface OrganizeMemberPersistence
        extends OrganizeMemberPersistencePort, OrganizeMembershipQueryPort {
}
