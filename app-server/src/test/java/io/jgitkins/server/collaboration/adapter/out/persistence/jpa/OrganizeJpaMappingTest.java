package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Mapping between the domain types and the JPA entities, without a database.
 *
 * <p>The interesting case is {@code PATH}. It is {@code NOT NULL UNIQUE} in the schema and the
 * domain has no path of its own, so {@code OrganizeDomainMapper} derives it from the organization
 * name for MyBatis. The JPA adapter has to derive it the same way or the two implementations stop
 * being interchangeable: writes would succeed under one selector and violate a unique constraint
 * under the other, and only at runtime.
 */
class OrganizeJpaMappingTest {

    private final OrganizeJpaRepository organizeJpaRepository = Mockito.mock(OrganizeJpaRepository.class);
    private final OrganizeMemberJpaRepository memberJpaRepository =
            Mockito.mock(OrganizeMemberJpaRepository.class);

    @Test
    void mapsOrganizeAndOrganizeMemberReferenceSlice() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 27, 10, 0);
        OrganizeJpaPersistenceAdapter organizeAdapter = new OrganizeJpaPersistenceAdapter(organizeJpaRepository);

        Organize organize = Organize.createWithoutEvent(
                OrganizeId.of(11L), OrganizeName.from("acme"), OwnerId.of(7L), "the description", now);
        Mockito.when(organizeJpaRepository.save(Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Organize roundTripped = organizeAdapter.save(organize);

        ArgumentCaptor<OrganizeJpaEntity> captor = ArgumentCaptor.forClass(OrganizeJpaEntity.class);
        Mockito.verify(organizeJpaRepository).save(captor.capture());
        OrganizeJpaEntity persisted = captor.getValue();

        assertThat(persisted.getId()).isEqualTo(11L);
        assertThat(persisted.getName()).isEqualTo("acme");
        assertThat(persisted.getPath())
                .as("PATH is NOT NULL UNIQUE and the domain has no path; both adapters must derive it "
                        + "from the name or they stop being interchangeable")
                .isEqualTo("acme");
        assertThat(persisted.getDescription()).isEqualTo("the description");
        assertThat(persisted.getOwnerId()).isEqualTo(7L);
        assertThat(persisted.getCreatedAt()).isEqualTo(now);

        assertThat(roundTripped.getId().getValue()).isEqualTo(11L);
        assertThat(roundTripped.getName().getValue()).isEqualTo("acme");
        assertThat(roundTripped.getOwnerId().getValue()).isEqualTo(7L);
        assertThat(roundTripped.getDescription()).isEqualTo("the description");

        OrganizeMemberJpaPersistenceAdapter memberAdapter =
                new OrganizeMemberJpaPersistenceAdapter(memberJpaRepository);
        Mockito.when(memberJpaRepository.findAllByOrganizeId(11L))
                .thenReturn(List.of(new OrganizeMemberJpaEntity(1L, 11L, 7L, "OWNER", now)));

        List<OrganizeMember> members = memberAdapter.findAllByOrganizeId(OrganizeId.of(11L));

        assertThat(members).hasSize(1);
        assertThat(members.get(0).getOrganizeId().getValue()).isEqualTo(11L);
        assertThat(members.get(0).getUserId().getValue()).isEqualTo(7L);
        assertThat(members.get(0).getRole()).isEqualTo(OrganizeMemberRole.OWNER);

        Mockito.when(memberJpaRepository.findByOrganizeIdAndUserId(11L, 7L))
                .thenReturn(Optional.of(new OrganizeMemberJpaEntity(1L, 11L, 7L, "OWNER", now)));
        assertThat(memberAdapter.findRoleByOrganizeIdAndUserId(11L, 7L)).contains(OrganizeMemberRole.OWNER);

        Mockito.when(memberJpaRepository.countByOrganizeIdAndRole(11L, "OWNER")).thenReturn(3L);
        assertThat(memberAdapter.countOwnersByOrganizeId(11L))
                .as("owners are counted in the database, matching the MyBatis count query rather than "
                        + "loading rows and filtering in memory")
                .isEqualTo(3L);
        Mockito.verify(memberJpaRepository).countByOrganizeIdAndRole(11L, "OWNER");
    }

    @Test
    void nullIdentifiersDoNotReachTheRepository() {
        OrganizeMemberJpaPersistenceAdapter memberAdapter =
                new OrganizeMemberJpaPersistenceAdapter(memberJpaRepository);

        assertThat(memberAdapter.existsByOrganizeIdAndUserId(null, MemberUserId.of(1L))).isFalse();
        assertThat(memberAdapter.findByOrganizeIdAndUserId(OrganizeId.of(1L), null)).isEmpty();
        assertThat(memberAdapter.findAllByOrganizeId(null)).isEmpty();
        assertThat(memberAdapter.findRoleByOrganizeIdAndUserId(null, 1L)).isEmpty();
        assertThat(memberAdapter.countOwnersByOrganizeId(null)).isZero();
        Mockito.verifyNoInteractions(memberJpaRepository);
    }
}
