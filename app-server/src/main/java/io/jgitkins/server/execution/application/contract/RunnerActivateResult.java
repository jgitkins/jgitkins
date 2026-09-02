package io.jgitkins.server.execution.application.contract;

import io.jgitkins.server.execution.application.contract.internal.RunnerExecutionConfig;
import io.jgitkins.server.execution.application.contract.internal.RunnerRuntimeConfig;

public record RunnerActivateResult(
        RunnerRuntimeConfig runtimeConfig,
        RunnerExecutionConfig executionConfig
) {
}
