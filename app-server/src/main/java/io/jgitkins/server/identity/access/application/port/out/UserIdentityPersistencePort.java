package io.jgitkins.server.identity.access.application.port.out;

import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import java.util.Optional;

public interface UserIdentityPersistencePort {
    Optional<UserIdentity> findByProvider(String providerName, String providerSub);

    UserIdentity save(UserIdentity identity);

    java.util.List<UserIdentity> findAllByUserId(Long userId);
}
