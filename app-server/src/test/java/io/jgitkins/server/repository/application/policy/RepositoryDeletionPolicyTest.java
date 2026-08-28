package io.jgitkins.server.repository.application.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.OwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Who may delete a repository, and which denial the caller sees.
 *
 * <p>Task 2.83. The replaced code returned early for every owner type other than USER, so an
 * organization-owned repository had no deletion authorization at all — and no test covered it, because
 * the one test that exercised that branch asserted the deletion succeeded. Each case here therefore
 * states both halves: whether the delete is refused, and whether the refusal is a 404 or a 403.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryDeletionPolicyTest {

    private static final long ORG_ID = 42L;
    private static final long OWNER_USER_ID = 7L;
    private static final long STRANGER_ID = 99L;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Mock
    private OrganizationMembershipPort organizationMembershipPort;

    @InjectMocks
    private RepositoryDeletionPolicy policy;

    // --- organization-owned ---------------------------------------------------------------------

    @Test
    void privateOrganizationRepository_isNotFoundForSomeoneWhoCannotSeeIt() {
        Repository repo = organizationRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, STRANGER_ID))
                .thenReturn(RepositoryPermission.none());

        assertThrows(RepositoryNotFoundException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));

        // The membership lookup must not run: reaching it would mean the existence check was skipped.
        verify(organizationMembershipPort, never())
                .findRoleByOrganizationIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void publicOrganizationRepository_isForbiddenRatherThanNotFoundForANonMember() {
        Repository repo = organizationRepository(RepositoryVisibility.PUBLIC);
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(ORG_ID, STRANGER_ID))
                .thenReturn(Optional.empty());

        // 403, not 404: the caller can already read this repository, so hiding it would be a lie that
        // protects nothing.
        assertThrows(RepositoryAccessDeniedException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));
    }

    @Test
    void organizationMember_canSeeTheRepositoryButMayNotDeleteIt() {
        Repository repo = organizationRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, STRANGER_ID))
                .thenReturn(new RepositoryPermission("ORGANIZATION_MEMBER", false, true));
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(ORG_ID, STRANGER_ID))
                .thenReturn(Optional.of(OrganizationMembershipRole.MEMBER));

        assertThrows(RepositoryAccessDeniedException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));
    }

    @Test
    void organizationMaintainer_mayNotDeleteEither() {
        Repository repo = organizationRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, STRANGER_ID))
                .thenReturn(new RepositoryPermission("ORGANIZATION_MAINTAINER", true, true));
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(ORG_ID, STRANGER_ID))
                .thenReturn(Optional.of(OrganizationMembershipRole.MAINTAINER));

        // Deliberate: MAINTAINER may push, and may not delete the repository it pushes to. Member
        // administration withholds the same role, and deletion is the larger authority of the two.
        assertThrows(RepositoryAccessDeniedException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));
    }

    @Test
    void organizationOwner_mayDelete() {
        Repository repo = organizationRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, STRANGER_ID))
                .thenReturn(new RepositoryPermission("ORGANIZATION_OWNER", true, true));
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(ORG_ID, STRANGER_ID))
                .thenReturn(Optional.of(OrganizationMembershipRole.OWNER));

        assertDoesNotThrow(() -> policy.validateCanDelete(STRANGER_ID, repo));
    }

    @Test
    void anonymousCaller_onAVisibleRepository_isUnauthenticatedRatherThanForbidden() {
        Repository repo = organizationRepository(RepositoryVisibility.PUBLIC);

        assertThrows(UnauthenticatedException.class, () -> policy.validateCanDelete(null, repo));
    }

    @Test
    void anonymousCaller_onAPrivateRepository_isNotFoundRatherThanUnauthenticated() {
        Repository repo = organizationRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, null))
                .thenReturn(RepositoryPermission.anonymous());

        // 401 would confirm the id names something real to an unauthenticated stranger.
        assertThrows(RepositoryNotFoundException.class, () -> policy.validateCanDelete(null, repo));
    }

    // --- user-owned -----------------------------------------------------------------------------

    @Test
    void userOwner_mayDeleteTheirOwnRepository() {
        Repository repo = userRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, OWNER_USER_ID))
                .thenReturn(new RepositoryPermission("OWNER", true, true));

        assertDoesNotThrow(() -> policy.validateCanDelete(OWNER_USER_ID, repo));
    }

    @Test
    void privateUserRepository_isNotFoundForAStranger() {
        Repository repo = userRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, STRANGER_ID))
                .thenReturn(RepositoryPermission.none());

        assertThrows(RepositoryNotFoundException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));
    }

    @Test
    void repositoryMemberOfAUserRepository_seesForbiddenRatherThanNotFound() {
        Repository repo = userRepository(RepositoryVisibility.PRIVATE);
        when(gitRepositoryAccessService.resolvePermission(repo, STRANGER_ID))
                .thenReturn(new RepositoryPermission("REPOSITORY_WRITER", true, true));

        // The split that matters: this caller can see the repository, so the refusal says forbidden.
        assertThrows(RepositoryAccessDeniedException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));
    }

    // --- data defects ---------------------------------------------------------------------------

    @Test
    void repositoryWithNoOwnerId_isRefusedRatherThanTreatedAsUnowned() {
        Repository repo = Repository.rehydrate(
                RepositoryId.of(1L), OwnerType.ORGANIZATION, null,
                RepositoryName.from("repo"), RepositoryPath.from("repo"), BranchName.of("main"),
                RepositoryVisibility.PUBLIC, null, "/org/repo.git", null, null, null, null);

        // The replaced code returned early here, which is one of the two branches that let the
        // organization case through unguarded.
        assertThrows(RepositoryNotFoundException.class,
                () -> policy.validateCanDelete(STRANGER_ID, repo));
        verify(gitRepositoryAccessService, never()).resolvePermission(any(Repository.class), anyLong());
    }

    // --- fixtures -------------------------------------------------------------------------------

    private static Repository organizationRepository(RepositoryVisibility visibility) {
        return Repository.rehydrate(
                RepositoryId.of(1L), OwnerType.ORGANIZATION, OwnerId.of(ORG_ID),
                RepositoryName.from("repo"), RepositoryPath.from("repo"), BranchName.of("main"),
                visibility, null, "/org/repo.git", null, null, null, null);
    }

    private static Repository userRepository(RepositoryVisibility visibility) {
        return Repository.rehydrate(
                RepositoryId.of(1L), OwnerType.USER, OwnerId.of(OWNER_USER_ID),
                RepositoryName.from("repo"), RepositoryPath.from("repo"), BranchName.of("main"),
                visibility, null, "/alice/repo.git", null, null, null, null);
    }
}
