package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;

public interface RepositoryMemberAddUseCase {
    void addRepositoryMember(RepositoryMemberAddCommand command);
}
