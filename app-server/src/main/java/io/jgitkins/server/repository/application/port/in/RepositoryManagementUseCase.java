package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

public interface RepositoryManagementUseCase {
    RepositoryResult create(RepositoryCreateCommand command);
    void deleteRepository(Long repositoryId);
}
