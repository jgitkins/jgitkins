package io.jgitkins.runner.infrastructure.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunnerJpaRepository extends JpaRepository<RunnerJpaEntity, Long> {

    /**
     * The one runner this process is.
     *
     * <p>A runner's local store describes the runner it belongs to, so the table holds either zero
     * rows (never activated) or one. The MyBatis path expressed the same read as an unfiltered select
     * ordered by {@code ID ASC} whose first element was taken, so ordering by id is kept rather than
     * relying on insertion order: if a second row ever appears, both paths pick the same one, and
     * whichever runner was activated first keeps its identity.
     */
    Optional<RunnerJpaEntity> findFirstByOrderByIdAsc();
}
