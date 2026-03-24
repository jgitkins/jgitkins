package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.model.OrganizeMember;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import io.jgitkins.server.infrastructure.persistence.model.OrganizeMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizeMemberDomainMapper {

    OrganizeMemberEntity toEntity(OrganizeMember member);

    default OrganizeMember toDomain(OrganizeMemberEntity entity) {
        if (entity == null) {
            return null;
        }
        return OrganizeMember.create(
                OrganizeId.of(entity.getOrganizeId()),
                UserId.of(entity.getUserId()),
                OrganizeMemberRole.from(entity.getRole()),
                entity.getJoinedAt()
        );
    }

    default Long map(OrganizeId organizeId) {
        return organizeId != null ? organizeId.getValue() : null;
    }

    default Long map(UserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    default String map(OrganizeMemberRole role) {
        return role != null ? role.name() : null;
    }
}
