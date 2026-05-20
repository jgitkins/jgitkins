package io.jgitkins.server.repository.infrastructure.mapper;

import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import io.jgitkins.server.repository.infrastructure.persistence.model.RepositoryMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RepositoryMemberDomainMapper {

    @Mapping(target = "repositoryId", source = "repositoryId.value")
    @Mapping(target = "userId", source = "userId.value")
    @Mapping(target = "role", source = "role")
    RepositoryMemberEntity toEntity(RepositoryMember member);

    default RepositoryMember toDomain(RepositoryMemberEntity entity) {
        if (entity == null) {
            return null;
        }
        return RepositoryMember.create(
                RepositoryId.of(entity.getRepositoryId()),
                UserId.of(entity.getUserId()),
                RepositoryMemberRole.valueOf(entity.getRole()),
                entity.getAddedAt());
    }
}
