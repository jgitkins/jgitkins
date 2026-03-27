package io.jgitkins.runner.infrastructure.http;

import io.jgitkins.runner.application.dto.RunnerExecutionConfigResult;
import io.jgitkins.runner.application.dto.RunnerRuntimeConfigResult;

public record ActivationResult(
        RunnerRuntimeConfigResult runtimeConfig,
        RunnerExecutionConfigResult executionConfig
) {
}
