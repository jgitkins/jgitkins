package io.jgitkins.server.application.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PushEventCommand {
    private final Long repositoryId;
    private final String namespace;
    private final String repoName;
    private final String branchName;
    private final boolean branchCreated;
    private final boolean branchDeleted;
    private final String commitHash;
    private final Long triggeredBy;
}
