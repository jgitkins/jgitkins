package io.jgitkins.server.application.port.out;

import java.util.Optional;

public interface PushEventRequestResolver {
    Optional<Long> resolveRequesterId();
}
