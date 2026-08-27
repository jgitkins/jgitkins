package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
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
        if (!permission.member()) {
            throw new ApplicationException(
                    ApplicationErrorCode.ACCESS_DENIED,
                    "Insufficient permission to access repository: " + repository.name());
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
            throw new ApplicationException(
                    ApplicationErrorCode.ACCESS_DENIED,
                    "Insufficient permission to commit to repository: " + namespace + "/" + repoName);
        }
    }
}
