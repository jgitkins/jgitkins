package io.jgitkins.server.execution.infrastructure.adapter.persistence;

import io.jgitkins.server.execution.domain.repository.RunnerRepository;
import io.jgitkins.server.execution.domain.aggregate.Runner;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Optional;

@Profile("test")
public class FakeRunnerPersistenceAdapter implements RunnerRepository {

    @Override
    public Runner save(Runner runner) {
        return runner.withId(1L);
    }

    @Override
    public void deleteById(Long runnerId) {

    }

    @Override
    public Optional<Runner> findById(Long runnerId) {
        return Optional.empty();
    }

    @Override
    public Optional<Runner> findByToken(String token) {
        return Optional.empty();
    }

    @Override
    public List<Runner> findAll() {
        return List.of();
    }
}
