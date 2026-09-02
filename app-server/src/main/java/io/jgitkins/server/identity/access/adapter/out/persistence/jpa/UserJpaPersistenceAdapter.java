package io.jgitkins.server.identity.access.adapter.out.persistence.jpa;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.adapter.out.persistence.UserPersistence;
import io.jgitkins.server.identity.access.application.internal.UserQueryResult;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.vo.UserAuthority;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * The JPA half of the identity user slice.
 *
 * <p>Serves the same two ports as {@code UserPersistenceAdapter} and is selected in its place when
 * the identity capability selector is {@code jpa}. Behaviour is held to the MyBatis implementation
 * rather than to what JPA makes convenient:
 *
 * <ul>
 *   <li>Email and username lookups trim the input and take the newest row, matching the MyBatis
 *       {@code order by id desc limit 1}. Returning the oldest row instead would change which
 *       account an ambiguous lookup resolves to.
 *   <li>{@code findAll} is newest-first for the same reason: the admin list order is observable.
 *   <li>{@code save} inserts when the id is null and updates otherwise, then reads the row back, so
 *       database-managed timestamps and generated ids reach the caller.
 *   <li>{@code AUTHORITY} and {@code STATUS} round-trip as enum names, matching what
 *       {@code UserDomainMapper} writes. A missing status falls back through
 *       {@code UserStatus.fromNullable}, and a missing authority to {@code USER}, exactly as the
 *       MyBatis mapper does.
 *   <li>Every failure is wrapped in {@code InfrastructureException} with
 *       {@code PERSISTENCE_OPERATION_FAILED}, so callers see one error contract under either
 *       selector.
 * </ul>
 */
@RequiredArgsConstructor
public class UserJpaPersistenceAdapter implements UserPersistence {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(Long id) {
        try {
            if (id == null) {
                return Optional.empty();
            }
            return userJpaRepository.findById(id).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user by id", e);
        }
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        try {
            if (id == null) {
                return Optional.empty();
            }
            return userJpaRepository.findByIdForUpdate(id).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during locked find user by id", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }
            return userJpaRepository.findFirstByEmailOrderByIdDesc(email.trim()).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user by email", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            return userJpaRepository.findFirstByUsernameOrderByIdDesc(username.trim()).map(this::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user by username", e);
        }
    }

    @Override
    public User save(User user) {
        try {
            UserJpaEntity saved = userJpaRepository.save(toEntity(user));
            return toDomain(saved);
        } catch (Exception e) {
            throw persistence("Database operation failed during save user", e);
        }
    }

    @Override
    public List<UserQueryResult> findAll() {
        try {
            return userJpaRepository.findAllByOrderByIdDesc().stream().map(this::toQueryResult).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during find all users", e);
        }
    }

    @Override
    public Optional<UserQueryResult> findUserDetailsById(Long userId) {
        try {
            if (userId == null) {
                return Optional.empty();
            }
            return userJpaRepository.findById(userId).map(this::toQueryResult);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user details by id", e);
        }
    }

    @Override
    public Optional<Long> findUserIdByUsername(String username) {
        try {
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            return userJpaRepository.findFirstByUsernameOrderByIdDesc(username.trim())
                    .map(UserJpaEntity::getId);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user id by username", e);
        }
    }

    @Override
    public Optional<String> findUsernameById(Long userId) {
        try {
            if (userId == null) {
                return Optional.empty();
            }
            return userJpaRepository.findById(userId).map(UserJpaEntity::getUsername);
        } catch (Exception e) {
            throw persistence("Database operation failed during find username by id", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return findUserIdByUsername(username).isPresent();
    }

    private UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getAuthority() != null ? user.getAuthority().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.rehydrate(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getAvatarUrl(),
                entity.getAuthority() != null ? UserAuthority.valueOf(entity.getAuthority()) : UserAuthority.USER,
                UserStatus.fromNullable(entity.getStatus()),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private UserQueryResult toQueryResult(UserJpaEntity entity) {
        return new UserQueryResult(entity.getId(), entity.getUsername(), entity.getEmail(),
                entity.getDisplayName(), entity.getAvatarUrl(),
                UserStatus.fromNullable(entity.getStatus()).name(), entity.getLastLoginAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
