package io.jgitkins.server.application.dto.command;

public record PushHookRequest(
        String gitDirPath,
        Long triggeredBy,
        String branchName,
        boolean branchCreated,
        boolean branchDeleted,
        String commitHash
) {
}
