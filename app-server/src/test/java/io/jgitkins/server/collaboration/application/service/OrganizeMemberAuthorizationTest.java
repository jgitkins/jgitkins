package io.jgitkins.server.collaboration.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.contract.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeMemberAuthorizationTest {

    @Mock private OrganizeMemberPersistencePort memberPersistencePort;
    @Mock private OrganizeMembershipQueryPort membershipQueryPort;
    @Mock private OrganizeRepository organizeRepository;

    @Test
    void nonOwnerCannotAddMember() {
        when(organizeRepository.findById(OrganizeId.of(1L)))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(io.jgitkins.server.collaboration.domain.aggregate.Organize.class)));
        when(membershipQueryPort.countOwnersByOrganizeId(1L)).thenReturn(1L);
        when(membershipQueryPort.findRoleByOrganizeIdAndUserId(1L, 7L))
                .thenReturn(Optional.of(OrganizeMemberRole.MEMBER));

        OrganizeMemberService service = new OrganizeMemberService(
                memberPersistencePort, membershipQueryPort, organizeRepository);

        assertThrows(OrganizeAccessDeniedException.class,
                () -> service.addOrganizeMember(new OrganizeMemberAddCommand(1L, 9L, OrganizeMemberRole.MEMBER, 7L)));
        verify(memberPersistencePort, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
