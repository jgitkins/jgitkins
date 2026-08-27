package io.jgitkins.server.identity.access.adapter.out.persistence;

import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;

/**
 * The user lifecycle port and the user read port, always served by one object.
 *
 * <p>Same reason as the collaboration slice: it gives the persistence selector a single type to
 * switch on inside one {@code @Bean} method, and it records that the aggregate lifecycle and its
 * read model must not come from different providers. Splitting them would let a cutover read through
 * JPA while writing through MyBatis.
 */
public interface UserPersistence extends UserRepository, UserQueryPort {
}
