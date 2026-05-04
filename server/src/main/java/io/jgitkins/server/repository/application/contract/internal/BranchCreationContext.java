package io.jgitkins.server.repository.application.contract.internal;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.domain.aggregate.Repository;

public record BranchCreationContext(
        BranchCreateCommand command,
        String namespace,
        Repository repository,
        String sourceBranch
) {
    public static BranchCreationContext of(
            BranchCreateCommand command,
            String namespace,
            Repository repository,
            String sourceBranch
    ) {
        return new BranchCreationContext(command, namespace, repository, sourceBranch);
    }

    public Long repositoryId() {
        return command.repositoryId();
    }

    public String branchName() {
        return command.branchName();
    }

    public boolean physicalCreationRequired() {
        return command.physicalCreationRequired();
    }

    public String repositoryName() {
        return repository.getName().getValue();
    }
}
