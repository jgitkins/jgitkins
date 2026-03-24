package io.jgitkins.server.application.dto;

import io.jgitkins.server.domain.model.vo.BranchName;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import io.jgitkins.server.domain.model.vo.RepositoryVisibility;

public record RepositoryCreationContext(OwnerType ownerType,
                                        OwnerId ownerId,
                                        RepositoryName repositoryName,
                                        RepositoryPath repositoryPath,
                                        BranchName defaultBranch,
                                        RepositoryVisibility visibility,
                                        String clonePath,
                                        String namespace) {
}
