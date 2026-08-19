package io.jgitkins.server.collaboration.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.application.service.OrganizeMemberService;
import io.jgitkins.server.collaboration.application.validate.OrganizeMemberValidator;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeMemberServiceTest {

    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;

    @Mock
    private OrganizeMembershipQueryPort organizeMemberQueryPort;

    private OrganizeMemberService service;

    @BeforeEach
    void setUp() {
        service = new OrganizeMemberService(
                organizeMemberPort,
                new OrganizeMemberValidator(organizeMemberQueryPort)
        );
    }

    @Test
    void addOrganizeMember_savesWhenNotExists() {
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        OrganizeMemberAddCommand command = new OrganizeMemberAddCommand(1L, 2L, OrganizeMemberRole.OWNER);

        service.addOrganizeMember(command);

        ArgumentCaptor<OrganizeMember> memberCaptor = ArgumentCaptor.forClass(OrganizeMember.class);
        verify(organizeMemberPort).save(memberCaptor.capture());
        assertEquals(OrganizeMemberRole.OWNER, memberCaptor.getValue().getRole());
    }

    @Test
    void addOrganizeMember_usesMemberRoleWhenRoleIsMissing() {
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        OrganizeMemberAddCommand command = new OrganizeMemberAddCommand(1L, 2L, null);

        service.addOrganizeMember(command);

        ArgumentCaptor<OrganizeMember> memberCaptor = ArgumentCaptor.forClass(OrganizeMember.class);
        verify(organizeMemberPort).save(memberCaptor.capture());
        assertEquals(OrganizeMemberRole.MEMBER, memberCaptor.getValue().getRole());
    }

    @Test
    void addOrganizeMember_throwsWhenAlreadyExists() {
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(1L, 2L)).thenReturn(Optional.of(OrganizeMemberRole.MEMBER));

        OrganizeMemberAddCommand command = new OrganizeMemberAddCommand(1L, 2L, OrganizeMemberRole.MEMBER);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.addOrganizeMember(command));
        verify(organizeMemberPort, never()).save(any());
    }

    @Test
    void removeOrganizeMember_deletesByOrganizeAndUser() {
        service.removeOrganizeMember(1L, 2L);

        verify(organizeMemberPort).deleteByOrganizeIdAndUserId(OrganizeId.of(1L), MemberUserId.of(2L));
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
