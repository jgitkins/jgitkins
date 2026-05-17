package io.jgitkins.server.application.dto.command;

public record PullRequestCreateCommand(
        String namespace,
        String repoName,
        String sourceBranch,
        String targetBranch
) {
}
