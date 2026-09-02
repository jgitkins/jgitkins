package io.jgitkins.runner.infrastructure.http;

import io.jgitkins.runner.application.contract.RunnerExecutionConfigResult;
import io.jgitkins.runner.application.contract.RunnerRuntimeConfigResult;

public record ActivationResult(
        RunnerRuntimeConfigResult runtimeConfig,
        RunnerExecutionConfigResult executionConfig
) {
}
