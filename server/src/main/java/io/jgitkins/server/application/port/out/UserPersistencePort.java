package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.model.User;
import java.util.Optional;

public interface UserPersistencePort {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    User save(User user);

    java.util.List<User> findAll();

    Optional<Long> findUserIdByUsername(String username);

}
