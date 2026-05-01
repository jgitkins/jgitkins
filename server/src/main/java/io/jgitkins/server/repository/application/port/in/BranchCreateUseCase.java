package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;

public interface BranchCreateUseCase {
    void createBranch(BranchCreateCommand command);
}
