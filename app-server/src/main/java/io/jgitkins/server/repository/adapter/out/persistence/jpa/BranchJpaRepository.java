package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, Long> {

    Optional<BranchJpaEntity> findFirstByRepositoryIdAndName(Long repositoryId, String name);

    List<BranchJpaEntity> findAllByRepositoryId(Long repositoryId);

    void deleteByRepositoryIdAndName(Long repositoryId, String name);
}
