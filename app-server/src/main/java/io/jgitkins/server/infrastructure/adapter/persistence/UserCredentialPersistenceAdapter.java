package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.UserCredentialPersistencePort;
import io.jgitkins.server.domain.model.UserCredential;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.UserCredentialDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.UserCredentialsEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.UserCredentialsEntity;
import io.jgitkins.server.infrastructure.persistence.model.UserCredentialsEntityCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCredentialPersistenceAdapter implements UserCredentialPersistencePort {

    private final UserCredentialsEntityMbgMapper userCredentialsEntityMbgMapper;
    private final UserCredentialDomainMapper userCredentialDomainMapper;

    @Override
    public UserCredential save(UserCredential credential) {
        try {
            UserCredentialsEntity entity = userCredentialDomainMapper.toEntity(credential);
            if (credential.getId() == null) {
                userCredentialsEntityMbgMapper.insertSelective(entity);
                return userCredentialDomainMapper.toDomain(entity);
            }
            userCredentialsEntityMbgMapper.updateByPrimaryKeySelective(entity);
            UserCredentialsEntity updated = userCredentialsEntityMbgMapper.selectByPrimaryKey(credential.getId());
            return userCredentialDomainMapper.toDomain(updated);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save user credential", e);
        }
    }

    @Override
    public Optional<UserCredential> findByUserIdAndProvider(Long userId, String provider) {
        try {
            if (userId == null || provider == null || provider.isBlank()) {
                return Optional.empty();
            }
            UserCredentialsEntityCondition condition = new UserCredentialsEntityCondition();
            condition.createCriteria()
                    .andUserIdEqualTo(userId)
                    .andProviderEqualTo(provider);
            condition.setOrderByClause("id desc limit 1");
            return userCredentialsEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(userCredentialDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find user credential by user id and provider", e);
        }
    }

    @Override
    public List<UserCredential> findAllByUserIdAndProvider(Long userId, String provider) {
        try {
            if (userId == null || provider == null || provider.isBlank()) {
                return List.of();
            }
            UserCredentialsEntityCondition condition = new UserCredentialsEntityCondition();
            condition.createCriteria()
                    .andUserIdEqualTo(userId)
                    .andProviderEqualTo(provider);
            condition.setOrderByClause("id desc");
            return userCredentialsEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .map(userCredentialDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all user credentials by user id and provider", e);
        }
    }

    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        try {
            if (id == null || userId == null) {
                return;
            }
            UserCredentialsEntityCondition condition = new UserCredentialsEntityCondition();
            condition.createCriteria()
                    .andIdEqualTo(id)
                    .andUserIdEqualTo(userId);
            userCredentialsEntityMbgMapper.deleteByCondition(condition);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during delete user credential", e);
        }
    }
}
