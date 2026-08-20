package io.jgitkins.server.repository.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BranchCreateRequest(
        @JsonAlias("name")
        String branchName,
        String sourceBranch
) {
}
