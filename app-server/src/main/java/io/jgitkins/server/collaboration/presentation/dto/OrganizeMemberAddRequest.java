package io.jgitkins.server.collaboration.presentation.dto;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;

public record OrganizeMemberAddRequest(
        Long userId,
        OrganizeMemberRole role
) {
}
