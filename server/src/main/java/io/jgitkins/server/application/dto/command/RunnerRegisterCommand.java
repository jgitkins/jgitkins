package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.domain.model.vo.RunnerScopeType;

public record RunnerRegisterCommand(
        String description,
        RunnerScopeType scopeType,
        Long targetId
) {
}
