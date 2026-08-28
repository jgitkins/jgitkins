package io.jgitkins.server.collaboration.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import io.jgitkins.server.collaboration.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.repository.OrganizeRepository;
import io.jgitkins.server.collaboration.application.validate.OrganizeValidator;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.entity.OrganizeMember;
import io.jgitkins.server.collaboration.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import io.jgitkins.server.shared.domain.exception.InvalidIdentifierException;
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
    private OrganizeRepository organizeRepository;

    @Mock
    private OrganizeMemberPersistencePort organizeMemberPort;

    @Mock
    private OrganizeMembershipQueryPort organizeMemberQueryPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private OrganizeApplicationMapper organizeApplicationMapper;

    private OrganizeService service;

    @BeforeEach
    void setUp() {
        service = new OrganizeService(
                organizeRepository,
                organizeMemberPort,
                domainEventPublisher,
                new OrganizeValidator(organizeRepository, organizeMemberQueryPort),
                organizeApplicationMapper);
    }

    @Test
    void createOrganize_savesWhenNameAndNamespaceAreAvailable() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc", 1L);

                when(organizeRepository.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizeRepository.save(any(Organize.class))).thenReturn(sampleOrganize(1L, "org", 1L));
        when(organizeApplicationMapper.toDto(any(Organize.class)))
                .thenReturn(new OrganizeCreationResult(null, "org", null, null, null, null));

        OrganizeCreationResult response = service.createOrganize(command);

        assertEquals("org", response.name());
        verify(organizeRepository).save(any(Organize.class));
    }

    @Test
    void createOrganize_publishesCreatedEventWithPersistedGeneratedId() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc", 1L);
        Organize persisted = sampleOrganize(42L, "org", 1L);

                when(organizeRepository.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizeRepository.save(any(Organize.class))).thenReturn(persisted);
        when(organizeApplicationMapper.toDto(persisted))
                .thenReturn(new OrganizeCreationResult(42L, "org", null, 1L, null, null));

        service.createOrganize(command);

        verify(domainEventPublisher).publish(argThat(events -> events.size() == 1
                && events.get(0) instanceof OrganizeCreatedEvent event
                && event.getOrganizeId().equals(OrganizeId.of(42L))));
    }

    @Test
    void createOrganize_bootstrapsCreatorAsOwnerMembership() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("bootstrap", "desc", 7L);
        Organize persisted = sampleOrganize(42L, "bootstrap", 7L);

                when(organizeRepository.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizeRepository.save(any(Organize.class))).thenReturn(persisted);
        when(organizeApplicationMapper.toDto(persisted))
                .thenReturn(new OrganizeCreationResult(42L, "bootstrap", null, 7L, null, null));

        service.createOrganize(command);

        verify(organizeMemberPort).save(argThat(member ->
                member.getOrganizeId().equals(OrganizeId.of(42L))
                        && member.getUserId().equals(MemberUserId.of(7L))
                        && member.getRole() == OrganizeMemberRole.OWNER));
    }
    @Test
    void createOrganize_throwsWhenOrganizeNameExists() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("duplicate", "desc", 1L);
                when(organizeRepository.findByName(any(OrganizeName.class)))
                .thenReturn(Optional.of(sampleOrganize(1L, "duplicate", 1L)));

        assertThrows(JgitkinsException.class, () -> service.createOrganize(command));
        verify(organizeRepository, never()).save(any(Organize.class));
    }

    /**
     * Task 2.95 moved this answer. The service used to throw OrganizeAccessDeniedException for a null
     * requester, which is a 403 saying "not allowed" about a value that is simply not an identifier.
     * OwnerId.of now rejects it with a mapped domain exception, and the "authenticated user required"
     * answer belongs to the adapter, where OrganizeController gives it as a 401.
     *
     * <p>What the service loses is the distinction between "no requester" and "invalid requester id".
     * Over HTTP that costs nothing: the controller rejects an absent principal before the command is
     * built. An internal caller passing null now reads "Identifier must be a positive value", which is
     * accurate about what happened even though a server bug is the real cause.
     */
    @Test
    void createOrganize_rejectsAMissingCurrentUserAsAnInvalidOwnerId() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc", null);
        assertThrows(InvalidIdentifierException.class, () -> service.createOrganize(command));
        verify(organizeRepository, never()).save(any(Organize.class));
    }


    @Test
    void createOrganize_usesAuthenticatedCurrentUserAsOwner() {
        OrganizeCreationCommand command = new OrganizeCreationCommand("org", "desc", 7L);

                when(organizeRepository.findByName(any(OrganizeName.class))).thenReturn(Optional.empty());
        when(organizeRepository.save(any(Organize.class))).thenReturn(sampleOrganize(1L, "org", 7L));
        when(organizeApplicationMapper.toDto(any(Organize.class)))
                .thenReturn(new OrganizeCreationResult(null, "org", null, 7L, null, null));

        service.createOrganize(command);

        verify(organizeRepository).save(argThat(organize ->
                organize.getOwnerId() != null && organize.getOwnerId().getValue().equals(7L)));
    }

    @Test
    void getOrganize_throwsWhenNotFound() {
        when(organizeRepository.findById(OrganizeId.of(99L))).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.getOrganize(99L));
    }

    @Test
    void getAccessibleOrganizes_returnsEmptyWhenCurrentUserMissing() {
        List<OrganizeCreationResult> results = service.getAccessibleOrganizes(null);

        assertEquals(0, results.size());
        verify(organizeRepository, never()).findAll();
    }

    @Test
    void getAccessibleOrganizes_allowsOwnerWithoutMembershipRow() {
        Organize owned = sampleOrganize(10L, "owned", 7L);

                when(organizeRepository.findAll()).thenReturn(List.of(owned));
        when(organizeApplicationMapper.toDto(owned))
                .thenReturn(new OrganizeCreationResult(10L, "owned", null, 7L, null, null));

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes(7L);

        assertEquals(List.of("owned"), results.stream().map(OrganizeCreationResult::name).toList());
        verify(organizeMemberQueryPort, never())
                .findRoleByOrganizeIdAndUserId(anyLong(), anyLong());
    }


    @Test
    void getAccessibleOrganizes_includesOwnedAndMemberOrganizes() {
        Organize owned = sampleOrganize(10L, "owned", 7L);
        Organize member = sampleOrganize(11L, "member", 20L);
        Organize other = sampleOrganize(12L, "other", 30L);

                when(organizeRepository.findAll()).thenReturn(List.of(owned, member, other));
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(11L, 7L)).thenReturn(Optional.of(OrganizeMemberRole.MEMBER));
        when(organizeMemberQueryPort.findRoleByOrganizeIdAndUserId(12L, 7L)).thenReturn(Optional.empty());
        when(organizeApplicationMapper.toDto(owned))
                .thenReturn(new OrganizeCreationResult(10L, "owned", null, null, null, null));
        when(organizeApplicationMapper.toDto(member))
                .thenReturn(new OrganizeCreationResult(11L, "member", null, null, null, null));

        List<OrganizeCreationResult> results = service.getAccessibleOrganizes(7L);

        assertEquals(2, results.size());
        assertEquals(List.of("owned", "member"), results.stream().map(OrganizeCreationResult::name).toList());
        verify(organizeMemberQueryPort).findRoleByOrganizeIdAndUserId(eq(11L), eq(7L));
        verify(organizeMemberQueryPort).findRoleByOrganizeIdAndUserId(eq(12L), eq(7L));
    }

    @Test
    void deleteOrganize_deletesWhenExists() {
        Organize existing = sampleOrganize(3L, "org3", 1L);
        when(organizeRepository.findById(OrganizeId.of(3L))).thenReturn(Optional.of(existing));

        service.deleteOrganize(3L);

        verify(organizeRepository).deleteById(OrganizeId.of(3L));
    }

    @Test
    void deleteOrganize_throwsWhenMissing() {
        when(organizeRepository.findById(OrganizeId.of(404L))).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.deleteOrganize(404L));
        verify(organizeRepository, never()).deleteById(any(OrganizeId.class));
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
