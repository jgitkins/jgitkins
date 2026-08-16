package io.jgitkins.server.collaboration.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrganizeDomainMapperTest {

    private final OrganizeDomainMapper mapper = Mappers.getMapper(OrganizeDomainMapper.class);

    @Test
    void toEntity_mapsDomainFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 11, 0);
        Organize organize = Organize.reconstruct(
                OrganizeId.of(7L),
                OrganizeName.from("alpha_team"),
                "  important  ",
                OwnerId.of(3L),
                createdAt,
                updatedAt);

        OrganizeEntity entity = mapper.toEntity(organize);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getName()).isEqualTo("alpha_team");
        assertThat(entity.getPath()).isEqualTo("alpha_team");
        assertThat(entity.getOwnerId()).isEqualTo(3L);
        assertThat(entity.getDescription()).isEqualTo("important");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void toDomain_restoresDomainFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 11, 0);
        OrganizeEntity entity = new OrganizeEntity();
        entity.setId(7L);
        entity.setName("alpha_team");
        entity.setPath("alpha_team");
        entity.setOwnerId(3L);
        entity.setDescription("important");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        Organize organize = mapper.toDomain(entity);

        assertThat(organize.getId()).isEqualTo(OrganizeId.of(7L));
        assertThat(organize.getName()).isEqualTo(OrganizeName.from("alpha_team"));
        assertThat(organize.getDescription()).isEqualTo("important");
        assertThat(organize.getOwnerId()).isEqualTo(OwnerId.of(3L));
        assertThat(organize.getCreatedAt()).isEqualTo(createdAt);
        assertThat(organize.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
