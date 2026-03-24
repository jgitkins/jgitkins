package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RepositoryMemberSummary {
    private final Long userId;
    private final RepositoryMemberRole role;
    private final LocalDateTime addedAt;
}
