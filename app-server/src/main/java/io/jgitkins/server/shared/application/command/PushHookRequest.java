package io.jgitkins.server.shared.application.command;

public record PushHookRequest(
        String gitDirPath,
        Long triggeredBy,
        String branchName,
        boolean branchCreated,
        boolean branchDeleted,
        String commitHash
) {
}
