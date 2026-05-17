package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.model.UserCredential;

import java.util.List;
import java.util.Optional;

public interface UserCredentialPersistencePort {
    UserCredential save(UserCredential credential);

    Optional<UserCredential> findByUserIdAndProvider(Long userId, String provider);

    List<UserCredential> findAllByUserIdAndProvider(Long userId, String provider);

    void deleteByIdAndUserId(Long id, Long userId);
}
