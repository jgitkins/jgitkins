package io.jgitkins.runner.application.dto;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RunnerActivateResult {

    String runnerToken;
    String masterBaseUrl;

    Duration pollInterval;
    Duration busyWaitInterval;

    String volumePath;
    String runnerImageName;
    String jenkinsfilePath;
    String jenkinsPluginConfigPath;
}
