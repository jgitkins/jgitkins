package io.jgitkins.runner.application.port.out;

import io.jgitkins.runner.domain.RunnerConfiguration;
import java.util.Optional;

public interface RunnerConfigurationPort {

    Optional<RunnerConfiguration> loadConfiguration();

    void save(RunnerConfiguration configuration);
}
