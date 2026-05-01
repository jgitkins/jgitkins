package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import java.util.List;
import java.util.Optional;

public interface BranchQueryPort {
    List<BranchSearchResult> findAllByRepositoryId(Long repositoryId);
    Optional<BranchSearchResult> findByRepositoryIdAndName(Long repositoryId, String branchName);
}
