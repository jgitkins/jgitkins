package io.jgitkins.server.identity.access.adapter.out.persistence;

import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.application.contract.result.UserQueryResult;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import io.jgitkins.server.identity.access.domain.repository.UserRepository;
import io.jgitkins.server.identity.access.domain.vo.UserStatus;
import io.jgitkins.server.identity.access.infrastructure.mapper.UserDomainMapper;
import io.jgitkins.server.identity.access.infrastructure.persistence.mapper.UserEntityMbgMapper;
import io.jgitkins.server.identity.access.infrastructure.persistence.model.UserEntity;
import io.jgitkins.server.identity.access.infrastructure.persistence.model.UserEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository, UserQueryPort {
    private final UserEntityMbgMapper userEntityMbgMapper;
    private final UserDomainMapper userDomainMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            if (email == null || email.isBlank()) return Optional.empty();
            UserEntityCondition condition = new UserEntityCondition();
            condition.createCriteria().andEmailEqualTo(email.trim());
            condition.setOrderByClause("id desc limit 1");
            return userEntityMbgMapper.selectByCondition(condition).stream().findFirst().map(userDomainMapper::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user by email", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            if (username == null || username.isBlank()) return Optional.empty();
            UserEntityCondition condition = new UserEntityCondition();
            condition.createCriteria().andUsernameEqualTo(username.trim());
            condition.setOrderByClause("id desc limit 1");
            return userEntityMbgMapper.selectByCondition(condition).stream().findFirst().map(userDomainMapper::toDomain);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user by username", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            if (id == null) return Optional.empty();
            return Optional.ofNullable(userDomainMapper.toDomain(userEntityMbgMapper.selectByPrimaryKey(id)));
        } catch (Exception e) {
            throw persistence("Database operation failed during find user by id", e);
        }
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        try {
            if (id == null) return Optional.empty();
            return Optional.ofNullable(userDomainMapper.toDomain(userEntityMbgMapper.selectByPrimaryKeyForUpdate(id)));
        } catch (Exception e) {
            throw persistence("Database operation failed during locked find user by id", e);
        }
    }

    @Override
    public User save(User user) {
        try {
            UserEntity entity = userDomainMapper.toEntity(user);
            if (user.getId() == null) {
                userEntityMbgMapper.insertSelective(entity);
                return userDomainMapper.toDomain(entity);
            }
            userEntityMbgMapper.updateByPrimaryKeySelective(entity);
            return userDomainMapper.toDomain(userEntityMbgMapper.selectByPrimaryKey(user.getId()));
        } catch (Exception e) {
            throw persistence("Database operation failed during save user", e);
        }
    }

    @Override
    public List<UserQueryResult> findAll() {
        try {
            UserEntityCondition condition = new UserEntityCondition();
            condition.setOrderByClause("id desc");
            return userEntityMbgMapper.selectByCondition(condition).stream().map(this::toQueryResult).toList();
        } catch (Exception e) {
            throw persistence("Database operation failed during find all users", e);
        }
    }

    @Override
    public Optional<UserQueryResult> findUserDetailsById(Long userId) {
        try {
            if (userId == null) return Optional.empty();
            return Optional.ofNullable(userEntityMbgMapper.selectByPrimaryKey(userId)).map(this::toQueryResult);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user details by id", e);
        }
    }

    @Override
    public Optional<Long> findUserIdByUsername(String username) {
        try {
            if (username == null || username.isBlank()) return Optional.empty();
            UserEntityCondition condition = new UserEntityCondition();
            condition.createCriteria().andUsernameEqualTo(username.trim());
            condition.setOrderByClause("id desc limit 1");
            return userEntityMbgMapper.selectByCondition(condition).stream().findFirst().map(UserEntity::getId);
        } catch (Exception e) {
            throw persistence("Database operation failed during find user id by username", e);
        }
    }

    @Override
    public Optional<String> findUsernameById(Long userId) {
        try {
            if (userId == null) return Optional.empty();
            UserEntity entity = userEntityMbgMapper.selectByPrimaryKey(userId);
            return Optional.ofNullable(entity).map(UserEntity::getUsername);
        } catch (Exception e) {
            throw persistence("Database operation failed during find username by id", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return findUserIdByUsername(username).isPresent();
    }

    private UserQueryResult toQueryResult(UserEntity entity) {
        return new UserQueryResult(entity.getId(), entity.getUsername(), entity.getEmail(), entity.getDisplayName(),
                entity.getAvatarUrl(), UserStatus.fromNullable(entity.getStatus()).name(), entity.getLastLoginAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private InfrastructureException persistence(String message, Exception cause) {
        return new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED, message, cause);
    }
}
