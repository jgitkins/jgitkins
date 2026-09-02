package io.jgitkins.server.repository.application.contract;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;

public record RepositoryMemberAddCommand(
        Long requesterUserId,
        Long repositoryId,
        Long userId,
        RepositoryMemberRole role
) {
}
