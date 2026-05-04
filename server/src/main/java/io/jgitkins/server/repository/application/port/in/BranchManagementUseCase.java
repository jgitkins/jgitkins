package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;

public interface BranchManagementUseCase {
    void createBranch(BranchCreateCommand command);
    void deleteBranch(Long repositoryId, String branchName);
}
