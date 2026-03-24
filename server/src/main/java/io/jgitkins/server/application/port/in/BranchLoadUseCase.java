package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.result.BranchSearchResult;

import java.util.List;

public interface BranchLoadUseCase {
    List<BranchSearchResult> getBranches(Long repositoryId);
    BranchSearchResult getBranch(Long repositoryId, String branchName);
}
