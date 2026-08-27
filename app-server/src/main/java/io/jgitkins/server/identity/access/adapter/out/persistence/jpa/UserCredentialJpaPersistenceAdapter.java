package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.identity.access.domain.entity.UserCredential;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The JPA half of the personal-access-token credential slice.
 *
 * <p>Lookups are newest-first to match the MyBatis {@code order by id desc}, which matters because
 * a user can hold several credentials for one provider and callers take the first.
 *
 * <p>{@code deleteByIdAndUserId} keeps both predicates. Deleting by id alone would compile, pass a
 * single-user test, and let one user delete another user's token - the user id is the authorization
 * check, not a redundant filter.
 */
@RequiredArgsConstructor
public class UserCredentialJpaPersistenceAdapter implements UserCredentialPersistencePort {

    private final UserCredentialJpaRepository userCredentialJpaRepository;

    @Override
    public UserCredential save(UserCredential credential) {
        try {
            UserCredentialJpaEntity saved = userCredentialJpaRepository.save(new UserCredentialJpaEntity(
                    credential.getId(),
                    credential.getUserId(),
                    credential.getProvider(),
                    credential.getName(),
                    credential.getDescription(),
                    credential.getPasswordHash(),
                    credential.getCreatedAt(),
                    credential.getUpdatedAt()));
            return toDomain(saved);
        } catch (Exception e) {
            throw persistence("Database operation failed during save user credential", e);
        }
    }

    @Override
    public Optional<UserCredential> findByUserIdAndProvider(Long userId, String provider) {
        try {
            if (userId == null || provider == null) {
                return Optional.empty();
            }
            return userCredentialJpaRepository
                    .findFirstByUserIdAndProviderOrderByIdDesc(userId, provider)
                    .map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user credential", e);
        }
    }

    @Override
    public List<UserCredential> findAllByUserIdAndProvider(Long userId, String provider) {
        try {
            if (userId == null || provider == null) {
                return List.of();
            }
            return userCredentialJpaRepository
                    .findAllByUserIdAndProviderOrderByIdDesc(userId, provider)
                    .stream().map(this::toDomain).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during find user credentials", e);
        }
    }

    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        try {
            if (id == null || userId == null) {
                return;
            }
            userCredentialJpaRepository.deleteByIdAndUserId(id, userId);
        } catch (Exception e) {
            throw persistence("Database operation failed during delete user credential", e);
        }
    }

    private UserCredential toDomain(UserCredentialJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserCredential.rehydrate(
                entity.getId(),
                entity.getUserId(),
                entity.getProvider(),
                entity.getName(),
                entity.getDescription(),
                entity.getPasswordHash(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
