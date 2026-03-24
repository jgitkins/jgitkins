package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganizeMemberSummary {
    private final Long userId;
    private final OrganizeMemberRole role;
    private final LocalDateTime joinedAt;
}
