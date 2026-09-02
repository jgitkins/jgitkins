package io.jgitkins.server.execution.application.contract.result;

import io.jgitkins.server.execution.application.internal.RunnerExecutionConfig;
import io.jgitkins.server.execution.application.internal.RunnerRuntimeConfig;

public record RunnerActivateResult(
        RunnerRuntimeConfig runtimeConfig,
        RunnerExecutionConfig executionConfig
) {
}
