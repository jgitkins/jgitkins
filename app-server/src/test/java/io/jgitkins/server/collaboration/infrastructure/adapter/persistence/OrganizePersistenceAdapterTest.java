package io.jgitkins.server.collaboration.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.collaboration.infrastructure.mapper.OrganizeDomainMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.mapper.OrganizeEntityMbgMapper;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeEntity;
import io.jgitkins.server.collaboration.infrastructure.persistence.model.OrganizeEntityCondition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizePersistenceAdapterTest {

    @Mock
    private OrganizeEntityMbgMapper organizeEntityMbgMapper;

    @Mock
    private OrganizeDomainMapper organizeDomainMapper;

    @InjectMocks
    private OrganizePersistenceAdapter adapter;

    @Test
    void save_persistsAndReturnsMappedDomain() {
        Organize organize = Organize.create(
                null,
                OrganizeName.from("alpha_team"),
                OwnerId.of(3L),
                "desc",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                java.time.Instant.parse("2026-01-01T10:00:00Z"));
        OrganizeEntity entity = new OrganizeEntity();
        Organize persisted = Organize.reconstruct(
                OrganizeId.of(10L),
                OrganizeName.from("alpha_team"),
                "desc",
                OwnerId.of(3L),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0));

        when(organizeDomainMapper.toEntity(organize)).thenReturn(entity);
        doAnswer(invocation -> {
            OrganizeEntity argument = invocation.getArgument(0);
            argument.setId(10L);
            return 1;
        }).when(organizeEntityMbgMapper).insertSelective(entity);
        when(organizeDomainMapper.toDomain(entity)).thenReturn(persisted);

        Organize result = adapter.save(organize);

        assertThat(result).isEqualTo(persisted);
        verify(organizeEntityMbgMapper).insertSelective(entity);
    }

    @Test
    void findByName_returnsMappedOrganizeWhenPresent() {
        OrganizeEntity entity = new OrganizeEntity();
        entity.setId(10L);
        entity.setName("alpha_team");
        entity.setPath("alpha_team");
        entity.setOwnerId(3L);
        entity.setDescription("desc");
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        Organize mapped = Organize.reconstruct(
                OrganizeId.of(10L),
                OrganizeName.from("alpha_team"),
                "desc",
                OwnerId.of(3L),
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0));

        when(organizeEntityMbgMapper.selectByCondition(org.mockito.ArgumentMatchers.any(OrganizeEntityCondition.class)))
                .thenReturn(List.of(entity));
        when(organizeDomainMapper.toDomain(entity)).thenReturn(mapped);

        Optional<Organize> result = adapter.findByName(OrganizeName.from("alpha_team"));

        assertThat(result).contains(mapped);
        ArgumentCaptor<OrganizeEntityCondition> captor = ArgumentCaptor.forClass(OrganizeEntityCondition.class);
        verify(organizeEntityMbgMapper).selectByCondition(captor.capture());
        assertThat(captor.getValue().getOredCriteria()).isNotEmpty();
    }

    @Test
    void findAll_mapsAllEntities() {
        OrganizeEntity first = new OrganizeEntity();
        first.setId(1L);
        first.setName("alpha");
        first.setPath("alpha");
        OrganizeEntity second = new OrganizeEntity();
        second.setId(2L);
        second.setName("beta");
        second.setPath("beta");
        Organize firstDomain = mock(Organize.class);
        Organize secondDomain = mock(Organize.class);

        when(organizeEntityMbgMapper.selectByCondition(org.mockito.ArgumentMatchers.any(OrganizeEntityCondition.class)))
                .thenReturn(List.of(first, second));
        when(organizeDomainMapper.toDomain(first)).thenReturn(firstDomain);
        when(organizeDomainMapper.toDomain(second)).thenReturn(secondDomain);

        List<Organize> result = adapter.findAll();

        assertThat(result).containsExactly(firstDomain, secondDomain);
    }
}
