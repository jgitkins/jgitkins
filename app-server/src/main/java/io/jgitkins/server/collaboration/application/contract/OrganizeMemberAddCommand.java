package io.jgitkins.server.collaboration.application.contract;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;

public record OrganizeMemberAddCommand(
        Long organizeId,
        Long userId,
        OrganizeMemberRole role,
        Long requesterUserId
) {
}
