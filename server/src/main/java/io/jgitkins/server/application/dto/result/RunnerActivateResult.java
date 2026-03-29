package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.application.dto.RunnerExecutionConfig;
import io.jgitkins.server.application.dto.RunnerRuntimeConfig;

public record RunnerActivateResult(
        RunnerRuntimeConfig runtimeConfig,
        RunnerExecutionConfig executionConfig
) {
}
