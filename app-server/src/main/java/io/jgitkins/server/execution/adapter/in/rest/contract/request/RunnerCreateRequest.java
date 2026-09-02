package io.jgitkins.server.execution.adapter.in.rest.contract.request;

import io.jgitkins.server.execution.domain.vo.RunnerScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RunnerCreateRequest(
        @NotBlank
        String description,

        @NotNull
        RunnerScopeType scopeType,

        Long targetId
) {
}
