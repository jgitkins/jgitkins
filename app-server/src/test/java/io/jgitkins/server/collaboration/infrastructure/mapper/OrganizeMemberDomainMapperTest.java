package io.jgitkins.server.collaboration.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeMemberEntity;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrganizeMemberDomainMapperTest {

    private final OrganizeMemberDomainMapper mapper = Mappers.getMapper(OrganizeMemberDomainMapper.class);

    @Test
    void toEntity_mapsDomainFields() {
        LocalDateTime joinedAt = LocalDateTime.of(2026, 1, 3, 12, 0);
        OrganizeMember member = OrganizeMember.create(
                OrganizeId.of(1L),
                UserId.of(2L),
                OrganizeMemberRole.OWNER,
                joinedAt);

        OrganizeMemberEntity entity = mapper.toEntity(member);

        assertThat(entity.getOrganizeId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(2L);
        assertThat(entity.getRole()).isEqualTo("OWNER");
        assertThat(entity.getJoinedAt()).isEqualTo(joinedAt);
    }

    @Test
    void toDomain_restoresDomainFields() {
        LocalDateTime joinedAt = LocalDateTime.of(2026, 1, 3, 12, 0);
        OrganizeMemberEntity entity = new OrganizeMemberEntity();
        entity.setOrganizeId(1L);
        entity.setUserId(2L);
        entity.setRole("owner");
        entity.setJoinedAt(joinedAt);

        OrganizeMember member = mapper.toDomain(entity);

        assertThat(member.getOrganizeId()).isEqualTo(OrganizeId.of(1L));
        assertThat(member.getUserId()).isEqualTo(UserId.of(2L));
        assertThat(member.getRole()).isEqualTo(OrganizeMemberRole.OWNER);
        assertThat(member.getJoinedAt()).isEqualTo(joinedAt);
    }
}
