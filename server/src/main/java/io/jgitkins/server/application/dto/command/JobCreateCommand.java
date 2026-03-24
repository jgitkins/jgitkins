package io.jgitkins.server.application.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JobCreateCommand {
    private final String repoName;
    private final Long repositoryId;
    private final String commitHash;
    private final String branchName;
    private final String pipelineFilePath;
    private final Long triggeredBy; // Users.id (FK)
}
