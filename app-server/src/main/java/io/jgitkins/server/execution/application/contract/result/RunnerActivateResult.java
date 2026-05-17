package io.jgitkins.server.execution.application.contract.result;

import io.jgitkins.server.execution.application.contract.result.RunnerExecutionConfig;
import io.jgitkins.server.execution.application.contract.result.RunnerRuntimeConfig;

public record RunnerActivateResult(
        RunnerRuntimeConfig runtimeConfig,
        RunnerExecutionConfig executionConfig
) {
}
