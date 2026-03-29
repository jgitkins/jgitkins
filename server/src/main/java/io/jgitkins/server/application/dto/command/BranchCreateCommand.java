package io.jgitkins.server.application.dto.command;

public record BranchCreateCommand(
        Long repositoryId,
        String branchName,
        String sourceBranch,
        boolean physicalCreationRequired
) {
}
