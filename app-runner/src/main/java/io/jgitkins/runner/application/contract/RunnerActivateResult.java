package io.jgitkins.runner.application.contract;

import java.time.Duration;

public record RunnerActivateResult(
        String runnerToken,
        String masterBaseUrl,
        Duration pollInterval,
        Duration busyWaitInterval,
        String volumePath,
        String runnerImageName,
        String jenkinsfilePath,
        String jenkinsPluginConfigPath
) {
}
