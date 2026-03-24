package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;
import lombok.Builder;

@Builder
public record RepositoryCreateCommand(
        String repoName,
        OwnerType ownerType,
        Long organizeId,
        String authorName,
        String authorEmail,
        String mainBranch,
        RepositoryVisibility visibility,
        String description,
        String credentialId,
        boolean readme,
        String message
) {
}
