package io.jgitkins.server.presentation.dto;

import io.jgitkins.server.domain.model.vo.RunnerScopeType;
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
