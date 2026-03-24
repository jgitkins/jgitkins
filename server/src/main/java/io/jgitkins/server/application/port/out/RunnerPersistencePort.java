package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.aggregate.Runner;

import java.util.List;
import java.util.Optional;

public interface RunnerPersistencePort {
    Runner save(Runner runner);
    void deleteById(Long runnerId);
    Runner update(Runner runner);

    Optional<Runner> findById(Long runnerId);
    Optional<Runner> findByToken(String token);
    List<Runner> findAll();

}
