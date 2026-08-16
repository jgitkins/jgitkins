package io.jgitkins.server.collaboration.infrastructure.mapper;

import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeMemberEntity;
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
                MemberUserId.of(entity.getUserId()),
                OrganizeMemberRole.from(entity.getRole()),
                entity.getJoinedAt()
        );
    }

    default Long map(OrganizeId organizeId) {
        return organizeId != null ? organizeId.getValue() : null;
    }

    default Long map(MemberUserId userId) {
        return userId != null ? userId.getValue() : null;
    }

    default String map(OrganizeMemberRole role) {
        return role != null ? role.name() : null;
    }
}
