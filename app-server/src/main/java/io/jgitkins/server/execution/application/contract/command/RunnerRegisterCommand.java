package io.jgitkins.server.execution.application.contract.command;

import io.jgitkins.server.execution.domain.vo.RunnerScopeType;

public record RunnerRegisterCommand(
        String description,
        RunnerScopeType scopeType,
        Long targetId
) {
}
