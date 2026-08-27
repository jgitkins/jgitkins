package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

public interface RepositoryManagementUseCase {
    RepositoryResult create(RepositoryCreateCommand command);
    /**
     * @param requesterUserId the authenticated caller, resolved once by the inbound adapter. First
     *     parameter by convention across every mutation in this context, so a caller cannot pass the
     *     repository id where the actor belongs and have it compile.
     */
    void deleteRepository(Long requesterUserId, Long repositoryId);
}
