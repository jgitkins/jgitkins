package io.jgitkins.server.identity.access.application.port.out;

import java.util.Optional;

public interface CurrentUserPort {
    Optional<Long> resolveCurrentUserId();
}
