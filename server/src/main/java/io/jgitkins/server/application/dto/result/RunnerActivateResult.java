package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.application.dto.RunnerExecutionConfig;
import io.jgitkins.server.application.dto.RunnerRuntimeConfig;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerActivateResult {
    private final RunnerRuntimeConfig runtimeConfig;
    private final RunnerExecutionConfig executionConfig;
}
