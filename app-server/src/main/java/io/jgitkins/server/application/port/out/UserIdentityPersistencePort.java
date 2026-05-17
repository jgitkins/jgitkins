package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.model.UserIdentity;
import java.util.Optional;

public interface UserIdentityPersistencePort {
    Optional<UserIdentity> findByProvider(String providerName, String providerSub);

    UserIdentity save(UserIdentity identity);

    java.util.List<UserIdentity> findAllByUserId(Long userId);
}
