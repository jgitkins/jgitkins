package io.jgitkins.server.collaboration.application.dto.result;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import java.time.LocalDateTime;

public record OrganizeMemberSummary(
        Long userId,
        OrganizeMemberRole role,
        LocalDateTime joinedAt
) {
}
