package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.application.mapper.OrganizeApplicationMapper;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.validate.OrganizeValidator;
import io.jgitkins.server.common.exception.JgitkinsException;
import io.jgitkins.server.domain.aggregate.Organize;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import io.jgitkins.server.domain.model.vo.UserId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeServiceTest {

    @Mock
    private OrganizePersistencePort organizePort;

    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;

    @Mock
    private CurrentUserPort currentUserPersistencePort;

    @Mock
    private OrganizeApplicationMapper organizeApplicationMapper;

    private OrganizeService service;

    @BeforeEach
    void setUp() {
        service = new OrganizeService(
                organizePort,
                currentUserPersistencePort,
                new OrganizeValidator(organizePort, organizeMemberPort),
                organizeApplicationMapper);
    }

    @Test
    void createOrganize_savesWhenNameAndNamespaceAreAvailable() {
        OrganizeCreationCommand command = OrganizeCreationCommand.builder()
                .name("org")
                .ownerId(1L)
                .description("desc")
                .build();

        when(organizePort.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizePort.save(any(Organize.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizeApplicationMapper.toDto(any(Organize.class)))
                .thenReturn(OrganizeCreationResult.builder().name("org").build());

        OrganizeCreationResult response = service.createOrganize(command);

        assertEquals("org", response.getName());
        verify(organizePort).save(any(Organize.class));
    }

    @Test
    void createOrganize_throwsWhenOrganizeNameExists() {
        OrganizeCreationCommand command = OrganizeCreationCommand.builder()
                .name("duplicate")
                .ownerId(1L)
                .description("desc")
                .build();
        when(organizePort.findByName(any(OrganizeName.class)))
                .thenReturn(Optional.of(sampleOrganize(1L, "duplicate", 1L)));

        assertThrows(JgitkinsException.class, () -> service.createOrganize(command));
        verify(organizePort, never()).save(any(Organize.class));
    }

    @Test
    void getOrganize_throwsWhenNotFound() {
        when(organizePort.findById(OrganizeId.of(99L))).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.getOrganize(99L));
    }

    @Test
    void getAccessibleOrganizes_returnsEmptyWhenCurrentUserMissing() {
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.empty());

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes();

        assertEquals(0, results.size());
        verify(organizePort, never()).findAll();
    }

    @Test
    void getAccessibleOrganizes_includesOwnedAndMemberOrganizes() {
        Organize owned = sampleOrganize(10L, "owned", 7L);
        Organize member = sampleOrganize(11L, "member", 20L);
        Organize other = sampleOrganize(12L, "other", 30L);

        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(7L));
        when(organizePort.findAll()).thenReturn(List.of(owned, member, other));
        when(organizeMemberPort.existsByOrganizeIdAndUserId(OrganizeId.of(11L), UserId.of(7L))).thenReturn(true);
        when(organizeMemberPort.existsByOrganizeIdAndUserId(OrganizeId.of(12L), UserId.of(7L))).thenReturn(false);
        when(organizeApplicationMapper.toDto(owned))
                .thenReturn(OrganizeCreationResult.builder().id(10L).name("owned").build());
        when(organizeApplicationMapper.toDto(member))
                .thenReturn(OrganizeCreationResult.builder().id(11L).name("member").build());

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes();

        assertEquals(2, results.size());
        assertEquals(List.of("owned", "member"), results.stream().map(OrganizeCreationResult::getName).toList());
        verify(organizeMemberPort).existsByOrganizeIdAndUserId(eq(OrganizeId.of(11L)), eq(UserId.of(7L)));
        verify(organizeMemberPort).existsByOrganizeIdAndUserId(eq(OrganizeId.of(12L)), eq(UserId.of(7L)));
    }

    @Test
    void deleteOrganize_deletesWhenExists() {
        Organize existing = sampleOrganize(3L, "org3", 1L);
        when(organizePort.findById(OrganizeId.of(3L))).thenReturn(Optional.of(existing));

        service.deleteOrganize(3L);

        verify(organizePort).deleteById(OrganizeId.of(3L));
    }

    @Test
    void deleteOrganize_throwsWhenMissing() {
        when(organizePort.findById(OrganizeId.of(404L))).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.deleteOrganize(404L));
        verify(organizePort, never()).deleteById(any(OrganizeId.class));
    }

    private Organize sampleOrganize(Long id, String name, Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        return Organize.reconstruct(
                OrganizeId.of(id),
                OrganizeName.from(name),
                name + " description",
                ownerId == null ? null : UserId.of(ownerId),
                now,
                now);
    }
}
