package io.jgitkins.runner.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunnerConfigJpaRepository extends JpaRepository<RunnerConfigJpaEntity, Long> {

    List<RunnerConfigJpaEntity> findAllByRunnerId(Long runnerId);

    Optional<RunnerConfigJpaEntity> findByRunnerIdAndConfigKey(Long runnerId, String configKey);
}
