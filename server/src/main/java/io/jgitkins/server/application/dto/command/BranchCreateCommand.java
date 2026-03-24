package io.jgitkins.server.application.dto.command;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BranchCreateCommand {
    private final Long repositoryId;
    private final String branchName;
    private final String sourceBranch;
    private final boolean physicalCreationRequired;
}
