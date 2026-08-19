package io.jgitkins.server.repository.application.port.out;

import java.util.Optional;

public interface RepositoryActorPort {
    Optional<Long> resolveCurrentUserId();
}
