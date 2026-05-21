package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipFactory;
import io.jgitkins.server.repository.application.validate.RepositoryMemberValidator;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryMemberServiceTest {

    @Mock
    private RepositoryMemberPersistencePort repositoryMemberPort;

    private RepositoryMemberService service;

    @BeforeEach
    void setUp() {
        RepositoryMemberValidator validator = new RepositoryMemberValidator(repositoryMemberPort);
        RepositoryMembershipFactory repositoryMembershipFactory = new RepositoryMembershipFactory();
        service = new RepositoryMemberService(repositoryMemberPort, validator, repositoryMembershipFactory);
    }

    @Test
    void addRepositoryMember_savesWithRequestedRoleWhenNotExists() {
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L))).thenReturn(false);

        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(1L, 2L, RepositoryMemberRole.MAINTAINER);

        service.addRepositoryMember(command);

        ArgumentCaptor<RepositoryMember> memberCaptor = ArgumentCaptor.forClass(RepositoryMember.class);
        verify(repositoryMemberPort).save(memberCaptor.capture());
        assertEquals(RepositoryMemberRole.MAINTAINER, memberCaptor.getValue().getRole());
    }

    @Test
    void addRepositoryMember_usesReaderRoleWhenRoleMissing() {
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L))).thenReturn(false);

        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(1L, 2L, null);

        service.addRepositoryMember(command);

        ArgumentCaptor<RepositoryMember> memberCaptor = ArgumentCaptor.forClass(RepositoryMember.class);
        verify(repositoryMemberPort).save(memberCaptor.capture());
        assertEquals(RepositoryMemberRole.READER, memberCaptor.getValue().getRole());
    }

    @Test
    void addRepositoryMember_doesNothingWhenAlreadyExists() {
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L))).thenReturn(true);

        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(1L, 2L, RepositoryMemberRole.WRITER);

        service.addRepositoryMember(command);

        verify(repositoryMemberPort, never()).save(any(RepositoryMember.class));
    }

    @Test
    void addRepositoryMember_throwsWhenCommandInvalid() {
        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(null));
        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(
                new RepositoryMemberAddCommand(1L, null, null)
        ));
    }

    @Test
    void removeRepositoryMember_deletesByRepositoryAndUser() {
        service.removeRepositoryMember(1L, 2L);

        verify(repositoryMemberPort).deleteByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L));
    }

    @Test
    void removeRepositoryMember_throwsWhenInputMissing() {
        assertThrows(JgitkinsException.class, () -> service.removeRepositoryMember(null, 2L));
        assertThrows(JgitkinsException.class, () -> service.removeRepositoryMember(1L, null));
    }

    @Test
    void getRepositoryMembers_mapsDomainToSummary() {
        LocalDateTime addedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        RepositoryMember member = RepositoryMember.create(
                RepositoryId.of(1L),
                UserId.of(2L),
                RepositoryMemberRole.WRITER,
                addedAt
        );
        when(repositoryMemberPort.findAllByRepositoryId(RepositoryId.of(1L))).thenReturn(List.of(member));

        List<RepositoryMemberSummary> result = service.getRepositoryMembers(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).userId());
        assertEquals(RepositoryMemberRole.WRITER, result.get(0).role());
        assertEquals(addedAt, result.get(0).addedAt());
    }

    @Test
    void getRepositoryMembers_throwsWhenRepositoryIdMissing() {
        assertThrows(JgitkinsException.class, () -> service.getRepositoryMembers(null));
    }
}
