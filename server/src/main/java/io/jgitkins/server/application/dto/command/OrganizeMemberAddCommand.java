package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrganizeMemberAddCommand {
    private final Long organizeId;
    private final Long userId;
    private final OrganizeMemberRole role;
}
