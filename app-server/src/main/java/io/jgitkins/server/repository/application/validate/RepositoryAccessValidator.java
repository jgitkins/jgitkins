package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryAccessValidator {

    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    /**
     * Authorizes a read of the result the route already loaded.
     *
     * <p>Task 2.65 changed both the argument type and the actor source. It used to take the
     * {@code Repository} aggregate and read {@code RepositoryActorPort}; it now takes the
     * {@code RepositoryResult} the caller already has and the requester it was given. Reusing the loaded
     * result matters beyond tidiness: a second lookup could observe a different row than the one about
     * to be returned, and would authorize against state the caller never sees.
     */
    public void validateReadAccess(RepositoryResult repository, Long requesterUserId) {
        // Null is allowed here and is not a rejection: a public repository is readable anonymously,
        // and the permission resolver is what decides that. Rejecting up front would break public
        // reads; substituting a sentinel id would make the resolver treat an anonymous caller as a
        // user who might coincidentally be a member.
        RepositoryPermission permission = gitRepositoryAccessUseCase.resolvePermission(
                repository, requesterUserId);
        // visibleOn, not member(). This used to read member() alone, which denied an authenticated
        // non-member reading a PUBLIC repository: the same request succeeded while logged out and
        // failed once logged in. See RepositoryPermission#visibleOn.
        if (!permission.visibleOn("PUBLIC".equals(repository.visibility()))) {
            // Not-found, not forbidden. Reaching here means the caller cannot see this repository at
            // all, and 403 would tell them from the status code alone that a private repository with
            // this id exists. The name must not appear in the message for the same reason.
            throw new RepositoryNotFoundException();
        }
    }

    /**
     * Read gate for the paths that hold a namespace and a name rather than a loaded result.
     *
     * <p>Mirrors {@link #validateCanCommit} in shape. The file paths went straight to git with no
     * check of any kind, so an anonymous caller could list every file name and path in a private
     * repository. Task 2.125 named the app-web route; the server answered the same thing directly.
     *
     * <p>{@code canRead} checks PUBLIC before membership, so an anonymous read of a public repository
     * still succeeds. That is why this takes a nullable requester instead of demanding one.
     *
     * @throws RepositoryNotFoundException when the caller cannot see the repository. Not-found rather
     *     than forbidden, and with no name in the message: a read denial means "cannot see it" by
     *     definition, and naming the repository in the body would put the leak back.
     */
    public void validateReadAccess(String namespace, String repoName, Long requesterUserId) {
        if (!gitRepositoryAccessUseCase.canRead(null, namespace.trim(), repoName.trim(), requesterUserId)) {
            throw new RepositoryNotFoundException();
        }
    }

    /**
     * @param requesterUserId supplied by the inbound adapter, task 2.64. It used to come from
     *     {@code RepositoryActorPort} inside this method, which made a write authorization decision
     *     depend on ambient security state and impossible to exercise for a chosen actor.
     */
    public void validateCanCommit(String namespace, String repoName, Long requesterUserId) {
        if (requesterUserId == null) {
            // Same code and message as when the port returned empty. The adapter rejects an
            // anonymous request earlier; this is the second line of defence and must not report a
            // different error than it did before.
            throw new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED, "Unauthenticated");
        }

        boolean allowed = gitRepositoryAccessUseCase.canWrite(
                null, namespace.trim(), repoName.trim(), requesterUserId);
        if (!allowed) {
            // Split on visibility, matching the deletion path. The extra read check runs only on the
            // denial branch, so the allowed path pays nothing for it.
            if (!gitRepositoryAccessUseCase.canRead(
                    null, namespace.trim(), repoName.trim(), requesterUserId)) {
                // Cannot see it: 403 here would confirm that namespace/repoName names something real.
                // canRead is also false when the repository does not resolve at all, which is the
                // same answer a caller should get for a name that does not exist.
                throw new RepositoryNotFoundException();
            }
            throw new ApplicationException(
                    ApplicationErrorCode.ACCESS_DENIED,
                    "Insufficient permission to commit to repository: " + namespace + "/" + repoName);
        }
    }
}
