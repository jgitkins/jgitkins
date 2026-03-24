package io.jgitkins.server.infrastructure.adapter.persistence;

import io.jgitkins.server.application.port.out.UserIdentityPersistencePort;
import io.jgitkins.server.domain.model.UserIdentity;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.mapper.UserIdentityDomainMapper;
import io.jgitkins.server.infrastructure.persistence.mapper.UserIdentitiesEntityMbgMapper;
import io.jgitkins.server.infrastructure.persistence.model.UserIdentitiesEntity;
import io.jgitkins.server.infrastructure.persistence.model.UserIdentitiesEntityCondition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserIdentityPersistenceAdapter implements UserIdentityPersistencePort {

    private final UserIdentitiesEntityMbgMapper userIdentitiesEntityMbgMapper;
    private final UserIdentityDomainMapper userIdentityDomainMapper;

    @Override
    public Optional<UserIdentity> findByProvider(String providerName, String providerSub) {
        try {
            UserIdentitiesEntityCondition condition = new UserIdentitiesEntityCondition();
            condition.createCriteria()
                    .andProviderNameEqualTo(providerName.trim())
                    .andProviderSubEqualTo(providerSub.trim());
            condition.setOrderByClause("id desc limit 1");
            return userIdentitiesEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .findFirst()
                    .map(userIdentityDomainMapper::toDomain);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find user identity by provider", e);
        }
    }

    @Override
    public UserIdentity save(UserIdentity identity) {
        try {
            UserIdentitiesEntity entity = userIdentityDomainMapper.toEntity(identity);
            if (identity.getId() == null) {
                userIdentitiesEntityMbgMapper.insertSelective(entity);
                return userIdentityDomainMapper.toDomain(entity);
            }
            userIdentitiesEntityMbgMapper.updateByPrimaryKeySelective(entity);
            UserIdentitiesEntity updated = userIdentitiesEntityMbgMapper.selectByPrimaryKey(identity.getId());
            return userIdentityDomainMapper.toDomain(updated);
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during save user identity", e);
        }
    }

    @Override
    public java.util.List<UserIdentity> findAllByUserId(Long userId) {
        try {
            if (userId == null) {
                return java.util.List.of();
            }
            UserIdentitiesEntityCondition condition = new UserIdentitiesEntityCondition();
            condition.createCriteria().andUserIdEqualTo(userId);
            condition.setOrderByClause("id desc");
            return userIdentitiesEntityMbgMapper.selectByCondition(condition)
                    .stream()
                    .map(userIdentityDomainMapper::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Database operation failed during find all user identities by user id", e);
        }
    }
}
