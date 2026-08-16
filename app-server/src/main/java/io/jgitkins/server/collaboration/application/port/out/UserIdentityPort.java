package io.jgitkins.server.collaboration.application.port.out;

import java.util.Optional;

/**
 * Collaboration's view of the identity capability it needs.
 */
public interface UserIdentityPort {

    Optional<Long> resolveCurrentActiveUserId();
}
