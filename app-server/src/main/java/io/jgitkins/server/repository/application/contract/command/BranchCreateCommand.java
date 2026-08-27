package io.jgitkins.server.repository.application.contract.command;

public record BranchCreateCommand(
        Long requesterUserId,
        Long repositoryId,
        String branchName,
        String sourceBranch,
        boolean physicalCreationRequired
) {
}
