package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import java.time.LocalDateTime;

public record OrganizeMemberSummary(
        Long userId,
        OrganizeMemberRole role,
        LocalDateTime joinedAt
) {
}
