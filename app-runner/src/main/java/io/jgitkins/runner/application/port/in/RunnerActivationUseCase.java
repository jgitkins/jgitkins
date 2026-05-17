package io.jgitkins.runner.application.port.in;

import io.jgitkins.runner.application.dto.RunnerActivateResult;

public interface RunnerActivationUseCase {

    RunnerActivateResult activate(String runnerToken, String activateEndpoint);
}
