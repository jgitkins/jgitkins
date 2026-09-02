package io.jgitkins.server.identity.access.adapter.out.persistence;

import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;

/**
 * The user lifecycle port and the user read port, always served by one object.
 *
 * <p>Same reason as the collaboration slice: introduced to give the persistence selector a single
 * type to switch on, and kept after it because what it records is that the aggregate lifecycle and
 * its read model must come from one object. Splitting them would let a write land in one
 * implementation and a read come from the other inside the same request.
 */
public interface UserPersistence extends UserRepository, UserQueryPort {
}
