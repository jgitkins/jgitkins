package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.identity.access.domain.entity.UserIdentity;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The JPA half of the federated-identity slice.
 *
 * <p>{@code findByProvider} is the OAuth login lookup: it resolves an external provider subject to a
 * local account. Both predicates are required and newest-first ordering matches MyBatis, because
 * resolving to the wrong row here logs someone into the wrong account.
 */
@Component
@RequiredArgsConstructor
public class UserIdentityJpaPersistenceAdapter implements UserIdentityPersistencePort {

    private final UserIdentityJpaRepository userIdentityJpaRepository;

    @Override
    public Optional<UserIdentity> findByProvider(String providerName, String providerSub) {
        try {
            if (providerName == null || providerName.isBlank() || providerSub == null || providerSub.isBlank()) {
                return Optional.empty();
            }
            return userIdentityJpaRepository
                    .findFirstByProviderNameAndProviderSubOrderByIdDesc(providerName.trim(), providerSub.trim())
                    .map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user identity by provider", e);
        }
    }

    @Override
    public UserIdentity save(UserIdentity identity) {
        try {
            UserIdentityJpaEntity saved = userIdentityJpaRepository.save(new UserIdentityJpaEntity(
                    identity.getId(),
                    identity.getUserId(),
                    identity.getProviderName(),
                    identity.getProviderSub(),
                    identity.getEmail(),
                    identity.isEmailVerified(),
                    identity.getName(),
                    identity.getAvatarUrl(),
                    identity.getCreatedAt(),
                    identity.getUpdatedAt()));
            return toDomain(saved);
        } catch (Exception e) {
            throw persistence("Database operation failed during save user identity", e);
        }
    }

    @Override
    public List<UserIdentity> findAllByUserId(Long userId) {
        try {
            if (userId == null) {
                return List.of();
            }
            return userIdentityJpaRepository.findAllByUserIdOrderByIdDesc(userId)
                    .stream().map(this::toDomain).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during find user identities", e);
        }
    }

    private UserIdentity toDomain(UserIdentityJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserIdentity.rehydrate(
                entity.getId(),
                entity.getUserId(),
                entity.getProviderName(),
                entity.getProviderSub(),
                entity.getEmail(),
                entity.isEmailVerified(),
                entity.getName(),
                entity.getAvatarUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
