package io.jgitkins.server.change.review.application.contract.command;

public record PullRequestCreateCommand(
        String namespace,
        String repoName,
        String sourceBranch,
        String targetBranch
) {
}
