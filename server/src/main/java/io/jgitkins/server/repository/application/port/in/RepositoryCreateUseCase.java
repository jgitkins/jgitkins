package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;

public interface RepositoryCreateUseCase {
    RepositoryResult create(RepositoryCreateCommand command);
}
