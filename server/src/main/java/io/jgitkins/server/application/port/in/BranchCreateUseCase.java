package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.BranchCreateCommand;

public interface BranchCreateUseCase {
    void createBranch(BranchCreateCommand command);
}
