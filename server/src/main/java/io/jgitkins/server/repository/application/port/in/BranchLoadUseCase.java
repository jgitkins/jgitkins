package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;

import java.util.List;

public interface BranchLoadUseCase {
    List<BranchSearchResult> loadBranches(Long repositoryId);
    BranchSearchResult loadBranch(Long repositoryId, String branchName);
}
