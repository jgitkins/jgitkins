package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;

public record OrganizeMemberAddCommand(
        Long organizeId,
        Long userId,
        OrganizeMemberRole role
) {
}
