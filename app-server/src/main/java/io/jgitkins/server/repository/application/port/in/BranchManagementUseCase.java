package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.BranchCreateCommand;

public interface BranchManagementUseCase {
    void createBranch(BranchCreateCommand command);
    /**
     * @param requesterUserId the authenticated caller, resolved once by the inbound adapter. First
     *     parameter by convention across every mutation in this context, so a caller cannot pass the
     *     repository id where the actor belongs and have it compile.
     */
    void deleteBranch(Long requesterUserId, Long repositoryId, String branchName);
}
