package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.RepositoryMemberAddCommand;

public interface RepositoryMemberManagementUseCase {
    void addRepositoryMember(RepositoryMemberAddCommand command);
    void removeRepositoryMember(Long repositoryId, Long userId);
}
