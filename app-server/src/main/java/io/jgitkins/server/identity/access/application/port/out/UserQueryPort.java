package io.jgitkins.server.identity.access.application.port.out;

import io.jgitkins.server.identity.access.application.internal.UserQueryResult;
import java.util.List;
import java.util.Optional;

public interface UserQueryPort {
    List<UserQueryResult> findAll();

    Optional<UserQueryResult> findUserDetailsById(Long userId);

    Optional<Long> findUserIdByUsername(String username);

    Optional<String> findUsernameById(Long userId);

    boolean existsByUsername(String username);
}
