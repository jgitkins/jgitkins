package io.jgitkins.server.application.dto.support;

import io.jgitkins.server.application.dto.command.PushEventCommand;

public record PushJobPlanRequest(String namespace,
                                 String repoName,
                                 String branchName,
                                 String commitHash) {

    public static PushJobPlanRequest from(PushEventCommand command) {
        return new PushJobPlanRequest(
                command.getNamespace(),
                command.getRepoName(),
                command.getBranchName(),
                command.getCommitHash()
        );
    }
}
