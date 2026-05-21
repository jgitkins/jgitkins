package io.jgitkins.server.shared.application.port.out;

import java.util.Optional;

public interface PushEventRequestResolver {
    Optional<Long> resolveRequesterId();
}
