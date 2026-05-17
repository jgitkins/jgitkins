package io.jgitkins.runner.application.mapper;

import io.jgitkins.runner.application.dto.RunnerActivateResult;
import io.jgitkins.runner.domain.RunnerConfiguration;

public final class RunnerActivateResultMapper {

    private RunnerActivateResultMapper() {
    }

    public static RunnerActivateResult fromConfiguration(RunnerConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        return new RunnerActivateResult(
                configuration.getRunnerToken(),
                configuration.getMasterBaseUrl(),
                configuration.getPollInterval(),
                configuration.getBusyWaitInterval(),
                null,
                null,
                null,
                null
        );
    }
}
