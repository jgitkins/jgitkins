package io.jgitkins.server.repository.domain.repository;

import io.jgitkins.server.repository.domain.entity.Branch;
import java.util.Optional;

public interface BranchRepository {
    void save(Branch branch);
    void delete(Branch branch);

    Optional<Branch> findByRepositoryIdAndName(Long repositoryId, String branchName);
}
