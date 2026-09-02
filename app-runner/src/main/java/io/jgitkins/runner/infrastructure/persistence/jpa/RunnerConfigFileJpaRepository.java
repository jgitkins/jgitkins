package io.jgitkins.runner.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunnerConfigFileJpaRepository extends JpaRepository<RunnerConfigFileJpaEntity, Long> {

    List<RunnerConfigFileJpaEntity> findAllByRunnerId(Long runnerId);

    Optional<RunnerConfigFileJpaEntity> findByRunnerIdAndFilename(Long runnerId, String filename);
}
