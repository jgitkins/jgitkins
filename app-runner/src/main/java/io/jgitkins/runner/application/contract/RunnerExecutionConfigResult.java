package io.jgitkins.runner.application.contract;

public record RunnerExecutionConfigResult(
        String runnerImageName,
        String jenkinsPluginConfig
) {
}
