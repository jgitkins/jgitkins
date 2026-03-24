package io.jgitkins.server.presentation.dto;

import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;

public record OrganizeMemberAddRequest(
        Long userId,
        OrganizeMemberRole role
) {
}
