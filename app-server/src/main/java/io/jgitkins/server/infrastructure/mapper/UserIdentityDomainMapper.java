package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.model.UserIdentity;
import io.jgitkins.server.infrastructure.persistence.model.UserIdentitiesEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserIdentityDomainMapper {

    UserIdentitiesEntity toEntity(UserIdentity identity);

    default UserIdentity toDomain(UserIdentitiesEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserIdentity.rehydrate(
                entity.getId(),
                entity.getUserId(),
                entity.getProviderName(),
                entity.getProviderSub(),
                entity.getEmail(),
                Boolean.TRUE.equals(entity.getEmailVerified()),
                entity.getName(),
                entity.getAvatarUrl(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
