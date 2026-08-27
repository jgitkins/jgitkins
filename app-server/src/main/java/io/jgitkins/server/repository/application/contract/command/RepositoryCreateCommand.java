package io.jgitkins.server.repository.application.contract.command;

import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import lombok.Builder;

@Builder
public record RepositoryCreateCommand(
        Long requesterUserId,
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
