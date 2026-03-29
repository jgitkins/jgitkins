package io.jgitkins.runner.application.dto;

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
