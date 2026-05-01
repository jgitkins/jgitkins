package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.application.contract.command.BranchCreationContext;
import java.io.IOException;

public interface BranchGitPort {
    void createBranch(BranchCreationContext context);
    void deleteBranch(String namespace, String repoName, String branchName);
    String getHeadCommitHash(String namespace, String repoName, String branchName) throws IOException;

}
