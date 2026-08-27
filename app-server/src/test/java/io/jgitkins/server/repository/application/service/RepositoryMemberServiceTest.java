package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.policy.RepositoryMemberManagementPolicy;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.membership.RepositoryMembershipFactory;
import io.jgitkins.server.repository.application.validate.RepositoryMemberValidator;
import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.repository.domain.model.RepositoryMember;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberUserId;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryMemberServiceTest {

    private static final long OWNER_ID = 7L;
    private static final long REPOSITORY_ID = 1L;

    @Mock
    private RepositoryMemberPersistencePort repositoryMemberPort;

    @Mock
    private RepositoryQueryPort repositoryQueryPort;

    private RepositoryMemberService service;

    @BeforeEach
    void setUp() {
        RepositoryMemberValidator validator = new RepositoryMemberValidator(repositoryMemberPort);
        RepositoryMembershipFactory repositoryMembershipFactory = new RepositoryMembershipFactory();
        // The policy is real, not mocked: it is the authorization this task added, and a mock would
        // let every member test pass without exercising it.
        RepositoryMemberManagementPolicy policy =
                new RepositoryMemberManagementPolicy(repositoryQueryPort);
        service = new RepositoryMemberService(
                repositoryMemberPort, validator, repositoryMembershipFactory, policy);
    }

    /**
     * Stubs the repository as user-owned by {@code OWNER_ID}.
     *
     * <p>Every member mutation now goes through {@code RepositoryMemberManagementPolicy}, so a test that
     * omits this is asserting the denial path whether it meant to or not. Called explicitly per test
     * rather than in {@code setUp} for exactly that reason: the tests that must be denied are the ones
     * that do not call it.
     */
    private void repositoryIsOwnedByRequester() {
        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.of(
                new RepositoryResult(REPOSITORY_ID, "USER", "repo", "owner/repo", "main", "PRIVATE",
                        null, OWNER_ID, null, "/owner/repo.git", null, false, null, null, null)));
    }

    @Test
    void addRepositoryMember_deniesANonOwner() {
        // The security fix this task carries: before it, any authenticated caller who knew a repository
        // id could grant themselves membership, and membership is what the read and commit paths check.
        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.of(
                new RepositoryResult(REPOSITORY_ID, "USER", "repo", "owner/repo", "main", "PRIVATE",
                        null, 999L, null, "/owner/repo.git", null, false, null, null, null)));

        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(
                new RepositoryMemberAddCommand(OWNER_ID, REPOSITORY_ID, 2L, RepositoryMemberRole.MAINTAINER)));

        verify(repositoryMemberPort, never()).save(any(RepositoryMember.class));
        verify(repositoryMemberPort, never())
                .existsByRepositoryIdAndUserId(any(RepositoryId.class), any(RepositoryMemberUserId.class));
    }

    @Test
    void addRepositoryMember_deniesAnOrganizationOwnedRepository() {
        // An organization-owned repository's ownerId is an organization id. Comparing it to a user id
        // would be comparing two different kinds of number, and would occasionally match by coincidence.
        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.of(
                new RepositoryResult(REPOSITORY_ID, "ORGANIZATION", "repo", "org/repo", "main", "PRIVATE",
                        null, OWNER_ID, null, "/org/repo.git", null, false, null, null, null)));

        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(
                new RepositoryMemberAddCommand(OWNER_ID, REPOSITORY_ID, 2L, RepositoryMemberRole.MAINTAINER)));
        verify(repositoryMemberPort, never()).save(any(RepositoryMember.class));
    }

    @Test
    void removeRepositoryMember_deniesANonOwnerBeforeDeleting() {
        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.of(
                new RepositoryResult(REPOSITORY_ID, "USER", "repo", "owner/repo", "main", "PRIVATE",
                        null, 999L, null, "/owner/repo.git", null, false, null, null, null)));

        assertThrows(JgitkinsException.class,
                () -> service.removeRepositoryMember(OWNER_ID, REPOSITORY_ID, 2L));

        verify(repositoryMemberPort, never())
                .deleteByRepositoryIdAndUserId(any(RepositoryId.class), any(RepositoryMemberUserId.class));
    }

    @Test
    void memberMutationsAreDeniedWhenTheRepositoryDoesNotExist() {
        when(repositoryQueryPort.loadRepository(REPOSITORY_ID)).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(
                new RepositoryMemberAddCommand(OWNER_ID, REPOSITORY_ID, 2L, RepositoryMemberRole.READER)));
        assertThrows(JgitkinsException.class,
                () -> service.removeRepositoryMember(OWNER_ID, REPOSITORY_ID, 2L));
    }

    @Test
    void addRepositoryMember_savesWithRequestedRoleWhenNotExists() {
        repositoryIsOwnedByRequester();
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), RepositoryMemberUserId.of(2L))).thenReturn(false);

        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(7L, 1L, 2L, RepositoryMemberRole.MAINTAINER);

        service.addRepositoryMember(command);

        ArgumentCaptor<RepositoryMember> memberCaptor = ArgumentCaptor.forClass(RepositoryMember.class);
        verify(repositoryMemberPort).save(memberCaptor.capture());
        assertEquals(RepositoryMemberRole.MAINTAINER, memberCaptor.getValue().getRole());
    }

    @Test
    void addRepositoryMember_usesReaderRoleWhenRoleMissing() {
        repositoryIsOwnedByRequester();
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), RepositoryMemberUserId.of(2L))).thenReturn(false);

        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(7L, 1L, 2L, null);

        service.addRepositoryMember(command);

        ArgumentCaptor<RepositoryMember> memberCaptor = ArgumentCaptor.forClass(RepositoryMember.class);
        verify(repositoryMemberPort).save(memberCaptor.capture());
        assertEquals(RepositoryMemberRole.READER, memberCaptor.getValue().getRole());
    }

    @Test
    void addRepositoryMember_doesNothingWhenAlreadyExists() {
        repositoryIsOwnedByRequester();
        when(repositoryMemberPort.existsByRepositoryIdAndUserId(RepositoryId.of(1L), RepositoryMemberUserId.of(2L))).thenReturn(true);

        RepositoryMemberAddCommand command = new RepositoryMemberAddCommand(7L, 1L, 2L, RepositoryMemberRole.WRITER);

        service.addRepositoryMember(command);

        verify(repositoryMemberPort, never()).save(any(RepositoryMember.class));
    }

    @Test
    void addRepositoryMember_throwsWhenCommandInvalid() {
        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(null));
        assertThrows(JgitkinsException.class, () -> service.addRepositoryMember(
                new RepositoryMemberAddCommand(7L, 1L, null, null)
        ));
    }

    @Test
    void removeRepositoryMember_deletesByRepositoryAndUser() {
        repositoryIsOwnedByRequester();
        service.removeRepositoryMember(7L, 1L, 2L);

        verify(repositoryMemberPort).deleteByRepositoryIdAndUserId(RepositoryId.of(1L), RepositoryMemberUserId.of(2L));
    }

    @Test
    void removeRepositoryMember_throwsWhenInputMissing() {
        assertThrows(JgitkinsException.class, () -> service.removeRepositoryMember(7L, null, 2L));
        assertThrows(JgitkinsException.class, () -> service.removeRepositoryMember(7L, 1L, null));
    }

    @Test
    void getRepositoryMembers_mapsDomainToSummary() {
        LocalDateTime addedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        RepositoryMember member = RepositoryMember.create(
                RepositoryId.of(1L),
                RepositoryMemberUserId.of(2L),
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
