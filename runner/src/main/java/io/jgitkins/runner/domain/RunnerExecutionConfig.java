package io.jgitkins.runner.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RunnerExecutionConfig {
    String runnerImageName;
    String jenkinsPluginConfig;
}
