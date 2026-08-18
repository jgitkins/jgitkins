package io.jgitkins.server.collaboration.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.exception.OrganizeAccessDeniedException;
import io.jgitkins.server.collaboration.application.mapper.OrganizeApplicationMapper;
import io.jgitkins.server.collaboration.application.service.OrganizeService;
import io.jgitkins.server.collaboration.application.port.out.DomainEventPublisher;
import io.jgitkins.server.collaboration.application.port.out.UserIdentityPort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
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
class OrganizeServiceTest {

    @Mock
    private OrganizePersistencePort organizePort;

    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;

    @Mock
    private OrganizeMembershipQueryPort organizeMemberQueryPort;

    @Mock
    private UserIdentityPort userIdentityPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrganizeApplicationMapper organizeApplicationMapper;

    private OrganizeService service;

    @BeforeEach
    void setUp() {
        service = new OrganizeService(
                organizePort,
                userIdentityPort,
                domainEventPublisher,
                new OrganizeValidator(organizePort, organizeMemberQueryPort),
                organizeApplicationMapper);
    }

    @Test
    void createOrganize_savesWhenNameAndNamespaceAreAvailable() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc");

        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.of(1L));
        when(organizePort.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizePort.save(any(Organize.class))).thenReturn(sampleOrganize(1L, "org", 1L));
        when(organizeApplicationMapper.toDto(any(Organize.class)))
                .thenReturn(new OrganizeCreationResult(null, "org", null, null, null, null));

        OrganizeCreationResult response = service.createOrganize(command);

        assertEquals("org", response.name());
        verify(organizePort).save(any(Organize.class));
    }

    @Test
    void createOrganize_publishesCreatedEventWithPersistedGeneratedId() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc");
        Organize persisted = sampleOrganize(42L, "org", 1L);

        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.of(1L));
        when(organizePort.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizePort.save(any(Organize.class))).thenReturn(persisted);
        when(organizeApplicationMapper.toDto(persisted))
                .thenReturn(new OrganizeCreationResult(42L, "org", null, 1L, null, null));

        service.createOrganize(command);

        verify(domainEventPublisher).publish(argThat(events -> events.size() == 1
                && events.get(0) instanceof OrganizeCreatedEvent event
                && event.getOrganizeId().equals(OrganizeId.of(42L))));
    }

    @Test
    void createOrganize_throwsWhenOrganizeNameExists() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("duplicate", "desc");
        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.of(1L));
        when(organizePort.findByName(any(OrganizeName.class)))
                .thenReturn(Optional.of(sampleOrganize(1L, "duplicate", 1L)));

        assertThrows(JgitkinsException.class, () -> service.createOrganize(command));
        verify(organizePort, never()).save(any(Organize.class));
    }

    @Test
    void createOrganize_throwsWhenCurrentUserIsMissing() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc");
        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.empty());

        assertThrows(OrganizeAccessDeniedException.class, () -> service.createOrganize(command));
        verify(organizePort, never()).save(any(Organize.class));
    }


    @Test
    void createOrganize_usesAuthenticatedCurrentUserAsOwner() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc");

        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.of(7L));
        when(organizePort.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizePort.save(any(Organize.class))).thenReturn(sampleOrganize(1L, "org", 7L));
        when(organizeApplicationMapper.toDto(any(Organize.class)))
                .thenReturn(new OrganizeCreationResult(null, "org", null, 7L, null, null));

        service.createOrganize(command);

        verify(organizePort).save(argThat(organize ->
                organize.getOwnerId() != null && organize.getOwnerId().getValue().equals(7L)));
    }

    @Test
    void getOrganize_throwsWhenNotFound() {
        when(organizePort.findById(OrganizeId.of(99L))).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.getOrganize(99L));
    }

    @Test
    void getAccessibleOrganizes_returnsEmptyWhenCurrentUserMissing() {
        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.empty());

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes();

        assertEquals(0, results.size());
        verify(organizePort, never()).findAll();
    }

    @Test
    void getAccessibleOrganizes_allowsOwnerWithoutMembershipRow() {
        Organize owned = sampleOrganize(10L, "owned", 7L);

        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.of(7L));
        when(organizePort.findAll()).thenReturn(List.of(owned));
        when(organizeApplicationMapper.toDto(owned))
                .thenReturn(new OrganizeCreationResult(10L, "owned", null, 7L, null, null));

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes();

        assertEquals(List.of("owned"), results.stream().map(OrganizeCreationResult::name).toList());
        verify(organizeMemberQueryPort, never())
                .existsByOrganizeIdAndUserId(any(OrganizeId.class), any(MemberUserId.class));
    }


    @Test
    void getAccessibleOrganizes_includesOwnedAndMemberOrganizes() {
        Organize owned = sampleOrganize(10L, "owned", 7L);
        Organize member = sampleOrganize(11L, "member", 20L);
        Organize other = sampleOrganize(12L, "other", 30L);

        when(userIdentityPort.resolveCurrentActiveUserId()).thenReturn(Optional.of(7L));
        when(organizePort.findAll()).thenReturn(List.of(owned, member, other));
        when(organizeMemberQueryPort.existsByOrganizeIdAndUserId(OrganizeId.of(11L), MemberUserId.of(7L))).thenReturn(true);
        when(organizeMemberQueryPort.existsByOrganizeIdAndUserId(OrganizeId.of(12L), MemberUserId.of(7L))).thenReturn(false);
        when(organizeApplicationMapper.toDto(owned))
                .thenReturn(new OrganizeCreationResult(10L, "owned", null, null, null, null));
        when(organizeApplicationMapper.toDto(member))
                .thenReturn(new OrganizeCreationResult(11L, "member", null, null, null, null));

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes();

        assertEquals(2, results.size());
        assertEquals(List.of("owned", "member"), results.stream().map(OrganizeCreationResult::name).toList());
        verify(organizeMemberQueryPort).existsByOrganizeIdAndUserId(eq(OrganizeId.of(11L)), eq(MemberUserId.of(7L)));
        verify(organizeMemberQueryPort).existsByOrganizeIdAndUserId(eq(OrganizeId.of(12L)), eq(MemberUserId.of(7L)));
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
                ownerId == null ? null : OwnerId.of(ownerId),
                now,
                now);
    }
}
