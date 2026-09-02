package io.jgitkins.server.execution.application.contract;

import io.jgitkins.server.execution.domain.vo.RunnerScopeType;

public record RunnerRegisterCommand(
        String description,
        RunnerScopeType scopeType,
        Long targetId
) {
}
