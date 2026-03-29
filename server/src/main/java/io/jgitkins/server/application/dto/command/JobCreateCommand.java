package io.jgitkins.server.application.dto.command;

public record JobCreateCommand(
        String repoName,
        Long repositoryId,
        String commitHash,
        String branchName,
        String pipelineFilePath,
        Long triggeredBy
) {
}
