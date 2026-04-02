package io.jgitkins.server.domain.repository;

import io.jgitkins.server.domain.Branch;
import java.util.Optional;

public interface BranchRepository {
    void save(Branch branch);
    void delete(Branch branch);

    Optional<Branch> findByRepositoryIdAndName(Long repositoryId, String branchName);
}
