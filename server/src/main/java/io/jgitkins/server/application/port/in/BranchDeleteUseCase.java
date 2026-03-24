package io.jgitkins.server.application.port.in;

public interface BranchDeleteUseCase {
    void deleteBranch(Long repositoryId, String branchName);
}
