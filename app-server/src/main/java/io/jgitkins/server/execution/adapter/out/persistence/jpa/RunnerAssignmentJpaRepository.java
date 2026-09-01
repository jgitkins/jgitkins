package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunnerAssignmentJpaRepository extends JpaRepository<RunnerAssignmentJpaEntity, Long> {

    /**
     * Newest-first, matching the MyBatis {@code order by assigned_at desc, id desc} the effective
     * scope uses.
     *
     * <p>The {@code IdDesc} tiebreak is not decoration. {@code ASSIGNED_AT} is declared
     * {@code timestamp} with no fractional part, so it stores whole seconds, and
     * {@code RunnerAssignmentDomainMapper} fills it with {@code LocalDateTime.now()}. Two rows written
     * in the same second are indistinguishable by that column and the winner is whatever the engine
     * happens to return. {@code ID} is auto-increment and monotonic, so it orders them by write.
     *
     * <p>It could not happen while the update branch wrote no assignment row. It can now, and the
     * first thing to hit it would have been the integration test for the change that made it
     * possible -- it creates a runner and changes its scope with no delay in between.
     */
    Optional<RunnerAssignmentJpaEntity> findFirstByRunnerIdOrderByAssignedAtDescIdDesc(Long runnerId);

    List<RunnerAssignmentJpaEntity> findAllByRunnerId(Long runnerId);

    void deleteByRunnerId(Long runnerId);
}
