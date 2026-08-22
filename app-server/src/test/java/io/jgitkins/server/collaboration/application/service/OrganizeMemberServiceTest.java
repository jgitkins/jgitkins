package io.jgitkins.server.collaboration.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.exception.OrganizeMemberNotFoundException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeMemberServiceTest {

    @Mock private OrganizeMemberPersistencePort organizeMemberPort;
    @Mock private OrganizeMembershipQueryPort organizeMemberQueryPort;
    @Mock private OrganizeRepository organizeRepository;

    private OrganizeMemberService service;

    @BeforeEach
    void setUp() {
        service = new OrganizeMemberService(organizeMemberPort, organizeMemberQueryPort, organizeRepository);
    }

    @Test
    void addOrganizeMember_ownerCanAssignRequestedRole() {
        when(organizeRepository.findById(OrganizeId.of(1L))).thenReturn(Optional.of(org.mockito.Mockito.mock(io.jgitkins.server.collaboration.domain.aggregate.Organize.class)));
        when(organizeMemberQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.OWNER));
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        service.addOrganizeMember(new OrganizeMemberAddCommand(1L, 2L, OrganizeMemberRole.MAINTAINER, 7L));

        verify(organizeMemberPort).save(org.mockito.ArgumentMatchers.argThat(member ->
                member.getRole() == OrganizeMemberRole.MAINTAINER && member.getUserId().equals(MemberUserId.of(2L))));
    }

    @Test
    void addOrganizeMember_nonOwnerCannotAdd() {
        when(organizeRepository.findById(OrganizeId.of(1L))).thenReturn(Optional.of(org.mockito.Mockito.mock(io.jgitkins.server.collaboration.domain.aggregate.Organize.class)));
        when(organizeMemberQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.MEMBER));

        assertThrows(OrganizeAccessDeniedException.class,
                () -> service.addOrganizeMember(new OrganizeMemberAddCommand(1L, 2L, OrganizeMemberRole.MEMBER, 7L)));
        verify(organizeMemberPort, never()).save(any());
    }

    @Test
    void addOrganizeMember_nullRoleDefaultsToMember() {
        when(organizeRepository.findById(OrganizeId.of(1L))).thenReturn(Optional.of(org.mockito.Mockito.mock(io.jgitkins.server.collaboration.domain.aggregate.Organize.class)));
        when(organizeMemberQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.OWNER));
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        service.addOrganizeMember(new OrganizeMemberAddCommand(1L, 2L, null, 7L));

        verify(organizeMemberPort).save(org.mockito.ArgumentMatchers.argThat(member -> member.getRole() == OrganizeMemberRole.MEMBER));
    }

    @Test
    void removeOrganizeMember_missingRequesterIsDenied() {
        assertThrows(OrganizeAccessDeniedException.class,
                () -> service.removeOrganizeMember(1L, null, 7L));
    }

    @Test
    void removeOrganizeMember_memberCanRemoveSelf() {
        when(organizeRepository.lockByIdForMembershipMutation(OrganizeId.of(1L))).thenReturn(null);
        when(organizeMemberQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.MEMBER));
        when(organizeMemberPort.findByOrganizeIdAndUserId(OrganizeId.of(1L), MemberUserId.of(7L)))
                .thenReturn(Optional.of(OrganizeMember.create(OrganizeId.of(1L), MemberUserId.of(7L), OrganizeMemberRole.MEMBER, null)));

        service.removeOrganizeMember(1L, 7L, 7L);

        verify(organizeMemberPort).deleteByOrganizeIdAndUserId(OrganizeId.of(1L), MemberUserId.of(7L));
    }

    @Test
    void removeOrganizeMember_soleOwnerCannotRemoveSelf() {
        when(organizeRepository.lockByIdForMembershipMutation(OrganizeId.of(1L))).thenReturn(null);
        when(organizeMemberQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.OWNER));
        when(organizeMemberPort.findByOrganizeIdAndUserId(OrganizeId.of(1L), MemberUserId.of(7L)))
                .thenReturn(Optional.of(OrganizeMember.create(OrganizeId.of(1L), MemberUserId.of(7L), OrganizeMemberRole.OWNER, null)));

        assertThrows(OrganizeAccessDeniedException.class, () -> service.removeOrganizeMember(1L, 7L, 7L));
        verify(organizeMemberPort, never()).deleteByOrganizeIdAndUserId(any(), any());
    }

    @Test
    void removeOrganizeMember_missingTargetIsNotFound() {
        when(organizeRepository.lockByIdForMembershipMutation(OrganizeId.of(1L))).thenReturn(null);
        when(organizeMemberQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.OWNER));
        when(organizeMemberPort.findByOrganizeIdAndUserId(OrganizeId.of(1L), MemberUserId.of(9L))).thenReturn(Optional.empty());

        assertThrows(OrganizeMemberNotFoundException.class, () -> service.removeOrganizeMember(1L, 7L, 9L));
    }

    @Test
    void getOrganizeMembers_mapsDomainToSummary() {
        LocalDateTime joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        OrganizeMember member = OrganizeMember.create(OrganizeId.of(1L), MemberUserId.of(2L), OrganizeMemberRole.OWNER, joinedAt);
        when(organizeMemberPort.findAllByOrganizeId(OrganizeId.of(1L))).thenReturn(List.of(member));

        List<OrganizeMemberSummary> result = service.getOrganizeMembers(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).userId());
        assertEquals(OrganizeMemberRole.OWNER, result.get(0).role());
        assertEquals(joinedAt, result.get(0).joinedAt());
    }
}
