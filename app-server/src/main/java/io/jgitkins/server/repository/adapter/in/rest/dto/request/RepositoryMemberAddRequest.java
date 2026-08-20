package io.jgitkins.server.repository.adapter.in.rest.dto.request;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;

public record RepositoryMemberAddRequest(
        Long userId,
        RepositoryMemberRole role
) {
}
