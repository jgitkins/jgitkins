package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.domain.model.vo.RepositoryMemberRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepositoryMemberAddCommand {
    private final Long repositoryId;
    private final Long userId;
    private final RepositoryMemberRole role;
}
