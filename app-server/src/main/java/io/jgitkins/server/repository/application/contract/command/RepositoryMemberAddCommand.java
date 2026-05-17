package io.jgitkins.server.repository.application.contract.command;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;

public record RepositoryMemberAddCommand(
        Long repositoryId,
        Long userId,
        RepositoryMemberRole role
) {
}
