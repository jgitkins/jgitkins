package io.jgitkins.server.identity.access.domain.repository;

import io.jgitkins.server.identity.access.domain.aggregate.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    User save(User user);
}
