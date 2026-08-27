package io.jgitkins.server.repository.application.validate;

import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
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

    private final RepositoryActorPort currentUserPersistencePort;
    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

    public void validateReadAccess(Repository repository) {
        Long userId = currentUserPersistencePort.resolveCurrentUserId().orElse(null);
        RepositoryPermission permission = gitRepositoryAccessUseCase.resolvePermission(repository, userId);
        if (!permission.member()) {
            throw new ApplicationException(
                    ApplicationErrorCode.ACCESS_DENIED,
                    "Insufficient permission to access repository: " + repository.getName());
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
