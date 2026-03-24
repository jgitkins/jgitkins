package io.jgitkins.server.application.dto.command;

import io.jgitkins.server.domain.model.vo.RunnerScopeType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RunnerRegisterCommand {
    private final String description;
    private final RunnerScopeType scopeType;
    private final Long targetId;
}
