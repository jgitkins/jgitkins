package io.jgitkins.server.repository.adapter.in.rest.contract.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * {@code branchName} mirrors {@code BranchName}, which rejects null and blank.
 *
 * <p>{@code sourceBranch} carries no constraint because it is optional by design:
 * {@code BranchCreationValidator:42} substitutes the default branch when it is null or blank.
 */
public record BranchCreateRequest(
        @JsonAlias("name")
        @NotBlank(message = "Branch name must not be blank")
        String branchName,
        String sourceBranch
) {
}
