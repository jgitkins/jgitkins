package io.jgitkins.server.repository.application.internal;

import io.jgitkins.server.shared.domain.model.vo.BranchName;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;

public record RepositoryCreationContext(OwnerType ownerType,
                                        RepositoryOwnerId ownerId,
                                        RepositoryName repositoryName,
                                        RepositoryPath repositoryPath,
                                        BranchName defaultBranch,
                                        RepositoryVisibility visibility,
                                        String clonePath,
                                        String namespace) {
}
