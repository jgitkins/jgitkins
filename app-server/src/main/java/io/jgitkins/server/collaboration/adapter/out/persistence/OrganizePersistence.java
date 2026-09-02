package io.jgitkins.server.collaboration.adapter.out.persistence;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;

/**
 * The two Organize outbound ports, always served by one object.
 *
 * <p>Introduced to give the persistence selector one type to switch on inside a single
 * {@code @Bean} method. The selector is gone and the pairing is kept, because the constraint it
 * expressed is not about providers: an aggregate lifecycle port and its read port must be served by
 * the same object. Two implementations, however consistent they looked, would let a write land in
 * one and a read come from the other inside the same request.
 */
public interface OrganizePersistence extends OrganizeRepository, OrganizeQueryPort {
}
