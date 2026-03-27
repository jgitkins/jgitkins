package io.jgitkins.runner.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerExecutionConfigResult {

    private final String runnerImageName;
    private final String jenkinsPluginConfig;
}
