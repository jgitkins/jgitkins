package io.jgitkins.server.repository.application.policy;

import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.shared.application.exception.UnauthenticatedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Who may delete a repository.
 *
 * <p>Task 2.83. Before this, deletion had no authorization for organization-owned repositories at all.
 * {@code RepositoryValidator.enforceDeletionPermission} returned early whenever the owner type was not
 * {@code USER}, and that method was the only permission check on the delete path: the service called
 * nothing else, {@code RepositoryProvisioner} only removes the git directory, {@code SecurityConfig}
 * is {@code permitAll}, and this application uses no method security. Any authenticated caller who knew
 * a repository id could delete another organization's repository, together with its git storage, with
 * no way to undo it. A unit test asserted the successful path of exactly that call, so the gap was
 * pinned as intended behaviour rather than merely uncovered.
 *
 * <p>The rule is the same one {@link io.jgitkins.server.repository.application.policy.RepositoryMemberManagementPolicy}
 * applies to member administration, and it is stated once per rule rather than once per endpoint:
 * a user-owned repository answers to its owner, and an organization-owned repository answers to the
 * organization's {@code OWNER}. Deleting a repository is a strictly larger authority than administering
 * its member list, so it cannot be granted to a role that member administration withholds.
 *
 * <h2>Not-found versus forbidden</h2>
 *
 * <p>Denial is split on visibility, not on the operation:
 *
 * <ul>
 *   <li>a repository the requester cannot see answers {@code 404} — otherwise the status code alone
 *       tells an unauthorized caller that a private repository with that id exists;</li>
 *   <li>a repository the requester can see but may not delete answers {@code 403} — a public repository
 *       is visible to everyone, and answering {@code 404} for one the caller can plainly read would be
 *       a worse lie than the one the {@code 404} is there to prevent.</li>
 * </ul>
 *
 * <p>This differs from {@code RepositoryMemberManagementPolicy}, which answers {@code 404} for every
 * denial including a public repository. That is not an inconsistency: a member list is administrative
 * metadata that is invisible to non-owners whatever the repository's visibility, so every denial there
 * is a visibility denial. Here the repository itself may be visible while the operation is not allowed,
 * and the two cases are distinguishable to the caller without leaking anything.
 *
 * <p>Visibility is not recomputed here. {@link GitRepositoryAccessService#resolvePermission(Repository, Long)}
 * already decides who may read a repository, including repository members and organization members of
 * every role, and a second copy of that rule would drift from the first.
 */
@Component
@RequiredArgsConstructor
public class RepositoryDeletionPolicy {

    private final GitRepositoryAccessService gitRepositoryAccessService;
    private final OrganizationMembershipPort organizationMembershipPort;

    /**
     * @throws RepositoryNotFoundException when the requester cannot see the repository
     * @throws UnauthenticatedException when the repository is visible but the requester is anonymous
     * @throws RepositoryAccessDeniedException when the requester can see it but may not delete it
     */
    public void validateCanDelete(Long requesterUserId, Repository repository) {
        // A repository with no owner type or owner id is a data defect, not a permission grant. The
        // replaced code returned early on both, which is how the organization branch went unguarded.
        if (repository == null || repository.getOwnerType() == null
                || repository.getOwnerId() == null || repository.getOwnerId().getValue() == null) {
            throw new RepositoryNotFoundException();
        }

        if (!isVisibleTo(repository, requesterUserId)) {
            // No-argument form on purpose: "Repository not found: 12" would confirm the id was real.
            throw new RepositoryNotFoundException();
        }

        // Reached only for a repository the caller may see, so answering 401 here cannot leak a
        // private repository's existence. In practice the controller rejects anonymous callers first;
        // this is the same rule stated where the decision is made.
        if (requesterUserId == null) {
            throw new UnauthenticatedException();
        }

        Long ownerId = repository.getOwnerId().getValue();
        boolean allowed = switch (repository.getOwnerType()) {
            case USER -> ownerId.equals(requesterUserId);
            case ORGANIZATION -> organizationMembershipPort
                    .findRoleByOrganizationIdAndUserId(ownerId, requesterUserId)
                    .filter(role -> role == OrganizationMembershipRole.OWNER)
                    .isPresent();
        };

        if (!allowed) {
            throw new RepositoryAccessDeniedException("Cannot delete this repository");
        }
    }

    private boolean isVisibleTo(Repository repository, Long requesterUserId) {
        if (repository.getVisibility() == RepositoryVisibility.PUBLIC) {
            return true;
        }
        return gitRepositoryAccessService.resolvePermission(repository, requesterUserId).member();
    }
}
