package io.jgitkins.server.application.port.out;

import io.jgitkins.server.application.dto.result.BranchSearchResult;
import java.util.List;
import java.util.Optional;

public interface BranchQueryPort {
    List<BranchSearchResult> findAllByRepositoryId(Long repositoryId);
    Optional<BranchSearchResult> findByRepositoryIdAndName(Long repositoryId, String branchName);
}
