package io.jgitkins.runner.application.port.out;

import io.jgitkins.runner.domain.RunnerConfiguration;

public interface RunnerActivatePort {

    RunnerConfiguration activateRunner(String runnerToken, String baseUrl);
}
