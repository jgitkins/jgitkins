package io.jgitkins.server.repository.application.contract.internal;

import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.InitialCommitOptions;

public record RepositoryCreationPlan(
        Repository repository,
        InitialCommitOptions initialCommitOptions
) {
}
