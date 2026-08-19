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

    public void validateCanCommit(String namespace, String repoName) {
        Long userId = currentUserPersistencePort.resolveCurrentUserId()
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.UNAUTHENTICATED, "Unauthenticated"));

        boolean allowed = gitRepositoryAccessUseCase.canWrite(null, namespace.trim(), repoName.trim(), userId);
        if (!allowed) {
            throw new ApplicationException(
                    ApplicationErrorCode.ACCESS_DENIED,
                    "Insufficient permission to commit to repository: " + namespace + "/" + repoName);
        }
    }
}
