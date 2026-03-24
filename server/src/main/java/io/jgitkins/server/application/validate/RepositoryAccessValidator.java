package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.port.out.CurrentUserPort;
import io.jgitkins.server.application.port.out.RepositoryPersistencePort;
import io.jgitkins.server.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.aggregate.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryAccessValidator {

    private final CurrentUserPort currentUserPersistencePort;
    private final RepositoryPersistencePort repositoryPort;
    private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

//    public void validateReadAccess(Long repositoryId) {
//        Repository repository = repositoryPort.findById(RepositoryId.of(repositoryId))
//                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.REPOSITORY_NOT_FOUND,
//                        "Repository not found"));
//
//        validateReadAccess(repository);
//    }

    public void validateReadAccess(Repository repository) {
        Long userId = currentUserPersistencePort.resolveCurrentUserId().orElse(null);
        boolean allowed = gitRepositoryAccessUseCase.canRead(null, null, null, userId); // TODO: implement correct check
        if (!allowed) {
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
