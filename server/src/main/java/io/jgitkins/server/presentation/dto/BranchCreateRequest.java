package io.jgitkins.server.presentation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BranchCreateRequest(
        @JsonAlias("name")
        String branchName,
        String sourceBranch
) {
}
