package io.jgitkins.server.presentation.dto;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;

public record RepositoryMemberAddRequest(
        Long userId,
        RepositoryMemberRole role
) {
}
