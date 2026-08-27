package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryMemberJpaRepository extends JpaRepository<RepositoryMemberJpaEntity, Long> {

    boolean existsByRepositoryIdAndUserId(Long repositoryId, Long userId);

    Optional<RepositoryMemberJpaEntity> findFirstByRepositoryIdAndUserId(Long repositoryId, Long userId);

    void deleteByRepositoryIdAndUserId(Long repositoryId, Long userId);

    List<RepositoryMemberJpaEntity> findAllByRepositoryIdOrderByAddedAtDesc(Long repositoryId);
}
