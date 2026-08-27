package io.jgitkins.server.collaboration.adapter.out.persistence;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;

/**
 * The two Organize outbound ports, always served by one object.
 *
 * <p>Exists so the persistence selector can decide between MyBatis and JPA inside a single
 * {@code @Bean} method. Without a common type the configuration would need one bean method per
 * implementation, both would be registered unconditionally, and the selector would have nothing to
 * switch. Declaring the pair together also states the constraint the slice depends on: an aggregate
 * lifecycle port and its read port must not be served by different implementations, or a cutover
 * could read through JPA while writing through MyBatis.
 */
public interface OrganizePersistence extends OrganizeRepository, OrganizeQueryPort {
}
