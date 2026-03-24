package io.jgitkins.server.presentation.dto;

import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;

public record RepositoryMemberAddRequest(
        Long userId,
        RepositoryMemberRole role
) {
}
