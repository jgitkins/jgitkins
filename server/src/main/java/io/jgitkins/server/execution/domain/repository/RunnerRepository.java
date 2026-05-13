package io.jgitkins.server.execution.domain.repository;

import io.jgitkins.server.execution.domain.aggregate.Runner;
import java.util.List;
import java.util.Optional;

public interface RunnerRepository {
    Runner save(Runner runner);

    void deleteById(Long runnerId);

    Optional<Runner> findById(Long runnerId);

    Optional<Runner> findByToken(String token);

    List<Runner> findAll();
}
