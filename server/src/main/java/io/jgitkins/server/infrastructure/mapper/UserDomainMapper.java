package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserStatus;
import io.jgitkins.server.infrastructure.persistence.model.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserDomainMapper {

    default UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setDisplayName(user.getDisplayName());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setAuthority(user.getAuthority() != null ? user.getAuthority().name() : null);
        entity.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }

    default User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.rehydrate(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getAvatarUrl(),
                entity.getAuthority() != null ? io.jgitkins.server.domain.model.UserAuthority.valueOf(entity.getAuthority()) : io.jgitkins.server.domain.model.UserAuthority.USER,
                UserStatus.fromNullable(entity.getStatus()),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
