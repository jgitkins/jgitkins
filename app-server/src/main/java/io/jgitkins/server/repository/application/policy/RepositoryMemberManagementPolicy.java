package io.jgitkins.server.repository.application.policy;

import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Who may add, remove, or list repository members.
 *
 * <p>Task 2.64. Before this, member mutation had no authorization at all: {@code RepositoryMemberService}
 * validated the identifiers in the command and wrote. Any authenticated caller who knew a repository id
 * could grant themselves membership of it, and membership is what the read and commit paths check. This
 * is the security fix inside an otherwise mechanical actor-injection task, which is why it is a mandatory
 * deliverable of the plan rather than an optional policy object.
 *
 * <p>Only the repository owner may manage members. For a user-owned repository the owner is a user id and
 * the comparison is direct. For an organization-owned repository the {@code ownerId} is an organization
 * id, so it is never compared to a user id — comparing two different kinds of number would occasionally
 * match by coincidence. The organization's members are looked up instead, and only role {@code OWNER}
 * acts for the organization.
 *
 * <p>Task 2.78 decided that. 2.64 denied every organization-owned repository and said so in this javadoc,
 * calling the membership lookup "a wider contract than this task owns" — a deferral, not a design. The
 * effect was that nobody could manage members of an organization-owned repository, the organization's own
 * OWNER included, which is a fail-closed outage of the feature organizations exist for. It is a gap.
 *
 * <p>OWNER only, rather than OWNER and MAINTAINER, for two reasons in this codebase. The collaboration
 * context already answers the same question for organization membership itself and answers it OWNER-only
 * ({@code OrganizeMemberService}, add and remove). And 2.64's rule is that administration belongs to the
 * owner: a repository MAINTAINER cannot manage members of a user-owned repository either. Granting
 * organization MAINTAINER would widen the rule rather than resolve who acts for an organization. Widening
 * later is a one-line change; narrowing after release is a breaking one.
 *
 * <p>This deliberately differs from {@code GitRepositoryAccessService}, which grants git write to every
 * organization role except MEMBER. Writing content and administering the member list are not the same
 * authority, and a MAINTAINER who can push but cannot change who else may push is the intended shape.
 *
 * <p>Takes {@link RepositoryQueryPort} rather than {@code RepositoryRepository}: this is a read to make a
 * decision, not an aggregate load to mutate, and the port it uses says which.
 */
@Component
@RequiredArgsConstructor
public class RepositoryMemberManagementPolicy {

    private final RepositoryQueryPort repositoryQueryPort;
    private final OrganizationMembershipPort organizationMembershipPort;

    /**
     * @throws RepositoryNotFoundException when the repository does not exist, preserving the existing 404
     * @throws RepositoryNotFoundException when the repository does not exist OR the requester may not
     *         manage its members. The two are deliberately indistinguishable (task 2.92).
     */
    public void validateCanManageMembers(Long requesterUserId, Long repositoryId) {
        if (repositoryId == null) {
            throw new RepositoryNotFoundException(repositoryId);
        }
        // The repository query happens before anything else, and before the membership query below.
        // Reading membership first would let a caller learn that a repository exists by timing or by
        // error shape. The organization lookup is reached only on the organization-owned branch, and
        // still precedes every repository-member read and write.
        var repository = repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        if (requesterUserId == null || repository.ownerId() == null) {
            throw denied();
        }

        OwnerType ownerType = ownerTypeOf(repository.ownerType());
        boolean allowed = switch (ownerType) {
            case USER -> repository.ownerId().equals(requesterUserId);
            case ORGANIZATION -> organizationMembershipPort
                    .findRoleByOrganizationIdAndUserId(repository.ownerId(), requesterUserId)
                    .filter(role -> role == OrganizationMembershipRole.OWNER)
                    .isPresent();
        };

        if (!allowed) {
            throw denied();
        }
    }

    /**
     * Normalizes through the shared value object rather than comparing the stored string, because
     * {@link OwnerType#from} accepts the {@code ORGANIZE} and {@code ORG} spellings that a row written
     * outside this application may carry. An owner type this application cannot name is a data problem,
     * and a member-management decision is the wrong place to surface it as a 500: it denies instead.
     */
    private OwnerType ownerTypeOf(String stored) {
        OwnerType ownerType;
        try {
            ownerType = OwnerType.from(stored);
        } catch (IllegalArgumentException unknownOwnerType) {
            throw denied();
        }
        if (ownerType == null) {
            throw denied();
        }
        return ownerType;
    }

    /**
     * Every denial answers not-found, not forbidden.
     *
     * <p>Task 2.92. Answering 404 when the repository does not exist and 403 when it exists but the
     * caller may not manage its members told an unauthorized caller that a private repository exists,
     * from the status code alone. GitHub answers 404 for a private repository the caller cannot see,
     * and this now does the same.
     *
     * <p>The message changes with it: saying "member management is not allowed" while answering 404
     * admits the repository exists and puts the leak straight back.
     *
     * <p>Scope is this method. {@code RepositoryAccessDeniedException} keeps its meaning everywhere
     * else, including {@code RepositoryValidator}, which answers repository creation and organization
     * membership. Editing the exception class instead would have turned unrelated endpoints into 404s.
     *
     * <p>Accepted cost: an organization MEMBER or MAINTAINER attempting member management now reads
     * "not found" rather than "not allowed". GitHub pays the same.
     */
    private RepositoryNotFoundException denied() {
        // The no-argument form on purpose: the (Long) form renders "Repository not found: null",
        // which announces that the id was not the reason.
        return new RepositoryNotFoundException();
    }
}
