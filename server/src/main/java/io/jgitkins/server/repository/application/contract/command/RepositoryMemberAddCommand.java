package io.jgitkins.server.repository.application.contract.command;

import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;

public record RepositoryMemberAddCommand(
        Long repositoryId,
        Long userId,
        RepositoryMemberRole role
) {
}
