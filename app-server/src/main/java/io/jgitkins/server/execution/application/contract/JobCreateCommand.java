package io.jgitkins.server.execution.application.contract;

public record JobCreateCommand(
        String repoName,
        Long repositoryId,
        String commitHash,
        String branchName,
        String pipelineFilePath,
        Long triggeredBy
) {
}
