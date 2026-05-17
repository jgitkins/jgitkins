package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.UserDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.UserEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.UserEntity;
import io.jgitkins.server.infrastructure.persistence.model.UserEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserEntityMbgMapper userEntityMbgMapper;
    private final UserDomainMapper userDomainMapper;

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            if (email == null || email.isBlank()) {
                return Optional.empty();
            }
            UserEntityCondition condition = new UserEntityCondition();
            condition.createCriteria().andEmailEqualTo(email.trim());
            condition.setOrderByClause("id desc limit 1");
            return userEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(userDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find user by email", e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            UserEntityCondition condition = new UserEntityCondition();
            condition.createCriteria().andUsernameEqualTo(username.trim());
            condition.setOrderByClause("id desc limit 1");
            return userEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(userDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find user by username", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            if (id == null) {
                return Optional.empty();
            }
            UserEntity entity = userEntityMbgMapper.selectByPrimaryKey(id);
            return Optional.ofNullable(userDomainMapper.toDomain(entity));
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find user by id", e);
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
            UserEntity updated = userEntityMbgMapper.selectByPrimaryKey(user.getId());
            return userDomainMapper.toDomain(updated);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save user", e);
        }
    }

    @Override
    public java.util.List<User> findAll() {
        try {
            UserEntityCondition condition = new UserEntityCondition();
            condition.setOrderByClause("id desc");
            return userEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .map(userDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all users", e);
        }
    }

    @Override
    public Optional<Long> findUserIdByUsername(String username) {
        try {
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            UserEntityCondition condition = new UserEntityCondition();
            condition.createCriteria().andUsernameEqualTo(username);
            condition.setOrderByClause("id desc limit 1");
            return userEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(entity -> entity.getId());
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find user id by username", e);
        }
    }

}
