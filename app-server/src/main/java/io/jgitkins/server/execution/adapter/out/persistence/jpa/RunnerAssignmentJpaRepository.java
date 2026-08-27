package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunnerAssignmentJpaRepository extends JpaRepository<RunnerAssignmentJpaEntity, Long> {

    /** Newest-first, matching the MyBatis {@code order by assigned_at desc} the effective scope uses. */
    Optional<RunnerAssignmentJpaEntity> findFirstByRunnerIdOrderByAssignedAtDesc(Long runnerId);

    List<RunnerAssignmentJpaEntity> findAllByRunnerId(Long runnerId);

    void deleteByRunnerId(Long runnerId);
}
