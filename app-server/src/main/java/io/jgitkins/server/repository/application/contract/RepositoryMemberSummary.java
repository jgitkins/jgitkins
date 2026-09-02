package io.jgitkins.server.repository.application.contract;

import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import java.time.LocalDateTime;

public record RepositoryMemberSummary(
        Long userId,
        RepositoryMemberRole role,
        LocalDateTime addedAt
) {
}
