package io.jgitkins.server.repository.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryMemberPersistencePort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.OwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Pins the contract {@link RepositoryDeletionPolicy} depends on, against the REAL
 * {@link GitRepositoryAccessService} rather than a mock of it.
 *
 * <p>Found by the engineering review of task 2.83. {@code RepositoryDeletionPolicyTest} stubs the
 * access service, so both sides of that seam were controlled by the same test: nothing verified that
 * the policy's reading of {@link RepositoryPermission} matches what the service actually produces.
 *
 * <p>The specific assumption at risk is the name. {@code member()} does not mean "is a member" — the
 * service returns {@code member=true} for an anonymous caller on a public repository, where the
 * caller is plainly not a member. It means "may read", and the deletion policy uses it as visibility.
 * If that field's meaning drifts, repository deletion silently changes who it lets in, and a test
 * that mocks the service cannot notice.
 */
class RepositoryVisibilityContractTest {

    private final RepositoryMemberPersistencePort repositoryMemberPort =
            mock(RepositoryMemberPersistencePort.class);
    private final OrganizationMembershipPort organizationMembershipPort =
            mock(OrganizationMembershipPort.class);
    private final GitRepositoryAccessService service = new GitRepositoryAccessService(
            mock(RepositoryLookupService.class), organizationMembershipPort, repositoryMemberPort);

    @Test
    void memberIsTrueForAnAnonymousCallerOnAPublicRepository() {
        RepositoryPermission permission = service.resolvePermission(repository(RepositoryVisibility.PUBLIC), null);

        // Not a member. May read. This is why the deletion policy may treat member() as visibility.
        assertThat(permission.member()).isTrue();
        assertThat(permission.role()).isEqualTo("PUBLIC_READ_ONLY");
    }

    @Test
    void memberIsFalseForAnAnonymousCallerOnAPrivateRepository() {
        RepositoryPermission permission = service.resolvePermission(repository(RepositoryVisibility.PRIVATE), null);

        assertThat(permission.member()).isFalse();
    }

    @Test
    void memberIsFalseForAnAuthenticatedNonMemberOnAPrivateRepository() {
        when(repositoryMemberPort.findByRepositoryIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(42L, 99L)).thenReturn(Optional.empty());

        RepositoryPermission permission = service.resolvePermission(repository(RepositoryVisibility.PRIVATE), 99L);

        assertThat(permission.member()).isFalse();
    }

    @Test
    void memberForAnAuthenticatedNonMemberOnAPublicRepository() {
        when(repositoryMemberPort.findByRepositoryIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(42L, 99L)).thenReturn(Optional.empty());

        RepositoryPermission permission = service.resolvePermission(repository(RepositoryVisibility.PUBLIC), 99L);

        // The trap. decide()'s PUBLIC short-circuit is `isPublic && userId == null` -- it covers the
        // ANONYMOUS caller only. An authenticated non-member on a public repository falls through to
        // none(), so member() is false. Any caller reading member() as "may read" without checking
        // PUBLIC first is wrong for exactly this case. both callers check PUBLIC first
        // now -- the deletion policy inline (so it can short-circuit) and the access validator via
        // RepositoryPermission#visibleOn. The validator was missing that branch until this was found.
        assertThat(permission.member()).isFalse();
    }

    private static Repository repository(RepositoryVisibility visibility) {
        return Repository.rehydrate(
                RepositoryId.of(1L), OwnerType.ORGANIZATION, OwnerId.of(42L),
                RepositoryName.from("repo"), RepositoryPath.from("repo"), BranchName.of("main"),
                visibility, null, "/org/repo.git", null, null, null, null);
    }
}
