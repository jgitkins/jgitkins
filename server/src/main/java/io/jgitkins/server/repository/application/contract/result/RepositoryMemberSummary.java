package io.jgitkins.server.repository.application.contract.result;

import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import java.time.LocalDateTime;

public record RepositoryMemberSummary(
        Long userId,
        RepositoryMemberRole role,
        LocalDateTime addedAt
) {
}
