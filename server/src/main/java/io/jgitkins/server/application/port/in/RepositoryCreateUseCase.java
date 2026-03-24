package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.RepositoryCreateCommand;
import io.jgitkins.server.application.dto.result.RepositoryResult;

public interface RepositoryCreateUseCase {
    RepositoryResult create(RepositoryCreateCommand command);
}
