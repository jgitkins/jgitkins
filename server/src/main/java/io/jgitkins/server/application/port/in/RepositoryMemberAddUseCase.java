package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.RepositoryMemberAddCommand;

public interface RepositoryMemberAddUseCase {
    void addRepositoryMember(RepositoryMemberAddCommand command);
}
