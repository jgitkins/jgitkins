package io.jgitkins.server.repository.application.port.out;

import java.util.Optional;

public interface UserNamespacePort {
    Optional<Long> findUserIdByUsername(String username);
}
