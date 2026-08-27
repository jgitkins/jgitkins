package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunnerJpaRepository extends JpaRepository<RunnerJpaEntity, Long> {

    Optional<RunnerJpaEntity> findFirstByTokenOrderByIdDesc(String token);
}
