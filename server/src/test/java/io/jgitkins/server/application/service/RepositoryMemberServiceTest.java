package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;
import io.jgitkins.server.application.dto.result.RepositoryMemberSummary;
import io.jgitkins.server.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.application.validate.RepositoryMemberValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.model.RepositoryMember;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
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
        service = new RepositoryMemberService(repositoryMemberPort, validator);
    }

    @Test
    void addRepositoryMember_savesWithRequestedRoleWhenNotExists() {
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L))).thenReturn(false);

        RepositoryMemberAddCommand command = RepositoryMemberAddCommand.builder()
                .repositoryId(1L)
                .userId(2L)
                .role(RepositoryMemberRole.MAINTAINER)
                .build();

        service.addRepositoryMember(command);

        ArgumentCaptor<RepositoryMember> memberCaptor = ArgumentCaptor.forClass(RepositoryMember.class);
        verify(repositoryMemberPort).save(memberCaptor.capture());
        assertEquals(RepositoryMemberRole.MAINTAINER, memberCaptor.getValue().getRole());
    }

    @Test
    void addRepositoryMember_usesReaderRoleWhenRoleMissing() {
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L))).thenReturn(false);

        RepositoryMemberAddCommand command = RepositoryMemberAddCommand.builder()
                .repositoryId(1L)
                .userId(2L)
                .build();

        service.addRepositoryMember(command);

        ArgumentCaptor<RepositoryMember> memberCaptor = ArgumentCaptor.forClass(RepositoryMember.class);
        verify(repositoryMemberPort).save(memberCaptor.capture());
        assertEquals(RepositoryMemberRole.READER, memberCaptor.getValue().getRole());
    }

    @Test
    void addRepositoryMember_doesNothingWhenAlreadyExists() {
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), UserId.of(2L))).thenReturn(true);

        RepositoryMemberAddCommand command = RepositoryMemberAddCommand.builder()
                .repositoryId(1L)
                .userId(2L)
                .role(RepositoryMemberRole.WRITER)
                .build();

        service.addRepositoryMember(command);

        verify(repositoryMemberPort, never()).save(any(RepositoryMember.class));
    }

    @Test
    void addRepositoryMember_throwsWhenCommandInvalid() {
        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(null));
        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(
                RepositoryMemberAddCommand.builder().repositoryId(1L).build()
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
        assertEquals(2L, result.get(0).getUserId());
        assertEquals(RepositoryMemberRole.WRITER, result.get(0).getRole());
        assertEquals(addedAt, result.get(0).getAddedAt());
    }

    @Test
    void getRepositoryMembers_throwsWhenRepositoryIdMissing() {
        assertThrows(JgitkinsException.class, () -> service.getRepositoryMembers(null));
    }
}
