package io.jgitkins.server.repository.application.policy;

import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Who may add or remove repository members.
 *
 * <p>Task 2.64. Before this, member mutation had no authorization at all: {@code RepositoryMemberService}
 * validated the identifiers in the command and wrote. Any authenticated caller who knew a repository id
 * could grant themselves membership of it, and membership is what the read and commit paths check. This
 * is the security fix inside an otherwise mechanical actor-injection task, which is why it is a mandatory
 * deliverable of the plan rather than an optional policy object.
 *
 * <p>Only the repository owner may manage members, and only for a user-owned repository is "owner" a
 * user id at all. An organization-owned repository's {@code ownerId} is an organization id, so comparing
 * it to a requester's user id would be comparing two different kinds of number — and would occasionally
 * match by coincidence. Those are denied here rather than silently allowed; organization-scoped member
 * management needs an organization membership lookup, which is a wider contract than this task owns.
 *
 * <p>Takes {@link RepositoryQueryPort} rather than {@code RepositoryRepository}: this is a read to make a
 * decision, not an aggregate load to mutate, and the port it uses says which.
 */
@Component
@RequiredArgsConstructor
public class RepositoryMemberManagementPolicy {

    private final RepositoryQueryPort repositoryQueryPort;

    /**
     * @throws RepositoryNotFoundException when the repository does not exist, preserving the existing 404
     * @throws RepositoryAccessDeniedException when the requester is not the owner, preserving 403
     */
    public void validateCanManageMembers(Long requesterUserId, Long repositoryId) {
        if (repositoryId == null) {
            throw new RepositoryNotFoundException(repositoryId);
        }
        // Exactly one query, and it happens before anything else reads or writes membership. Loading the
        // repository after a membership read would let a caller learn that a repository exists by timing
        // or by error shape.
        var repository = repositoryQueryPort.loadRepository(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));

        boolean userOwned = OwnerType.USER.name().equals(repository.ownerType());
        boolean isOwner = userOwned
                && repository.ownerId() != null
                && requesterUserId != null
                && repository.ownerId().equals(requesterUserId);

        if (!isOwner) {
            throw new RepositoryAccessDeniedException("Repository member management is not allowed");
        }
    }
}
