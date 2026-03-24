package io.jgitkins.server.application.port.out;

import io.jgitkins.server.application.dto.command.BranchCreationContext;

public interface BranchGitPort {
    void createBranch(BranchCreationContext context);
    void deleteBranch(String namespace, String repoName, String branchName);

}
