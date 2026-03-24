package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.Branch;

import java.util.List;
import java.util.Optional;

public interface BranchPersistencePort {
    void save(Branch branch);
    void deleteByRepositoryIdAndName(Long repositoryId, String branchName);

    Optional<Branch> findByRepositoryIdAndName(Long repositoryId, String branchName);
    List<Branch> findAllByRepositoryId(Long repositoryId);
}
