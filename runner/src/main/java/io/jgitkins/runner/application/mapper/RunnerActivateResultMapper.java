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
        return RunnerActivateResult.builder()
                                   .runnerToken(configuration.getRunnerToken())
                                   .masterBaseUrl(configuration.getMasterBaseUrl())
                                   .pollInterval(configuration.getPollInterval())
                                   .busyWaitInterval(configuration.getBusyWaitInterval())
//                                   .volumePath(configuration.getVolumePath())
//                                   .runnerImageName(configuration.getRunnerImageName())
//                                   .jenkinsfilePath(configuration.getJenkinsfilePath())
//                                   .jenkinsPluginConfigPath(configuration.getJenkinsPluginConfigPath())
                                   .build();
    }
}
