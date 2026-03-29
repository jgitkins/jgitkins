package io.jgitkins.runner.application.dto;

public record RunnerExecutionConfigResult(
        String runnerImageName,
        String jenkinsPluginConfig
) {
}
