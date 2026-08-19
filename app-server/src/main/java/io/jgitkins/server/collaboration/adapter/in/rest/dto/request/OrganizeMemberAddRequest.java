package io.jgitkins.server.collaboration.adapter.in.rest.dto.request;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;

public record OrganizeMemberAddRequest(
        Long userId,
        OrganizeMemberRole role
) {
}
