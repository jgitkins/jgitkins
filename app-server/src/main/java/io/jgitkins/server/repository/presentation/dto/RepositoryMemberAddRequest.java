package io.jgitkins.server.repository.presentation.dto;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;

public record RepositoryMemberAddRequest(
        Long userId,
        RepositoryMemberRole role
) {
}
