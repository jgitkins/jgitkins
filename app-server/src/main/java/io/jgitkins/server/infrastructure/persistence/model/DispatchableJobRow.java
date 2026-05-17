package io.jgitkins.server.infrastructure.persistence.model;

import java.time.LocalDateTime;

public record DispatchableJobRow(Long jobId,
                                 Long repositoryId,
                                 String commitHash,
                                 String branchName,
                                 Long triggeredBy,
                                 LocalDateTime jobCreatedAt,
                                 String repositoryOwnerType,
                                 Long repositoryOwnerId,
                                 String repositoryClonePath) {
}
