package io.jgitkins.server.application.port.out;

import java.util.Optional;

public interface CurrentUserPort {
    Optional<Long> resolveCurrentUserId();
}
