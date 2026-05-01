package io.jgitkins.server.repository.application.port.in;

public interface BranchDeleteUseCase {
    void deleteBranch(Long repositoryId, String branchName);
}
