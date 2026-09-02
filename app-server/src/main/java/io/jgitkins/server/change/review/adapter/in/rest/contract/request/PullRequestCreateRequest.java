package io.jgitkins.server.change.review.adapter.in.rest.contract.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Constrained against {@code BranchName}'s rule (null and blank rejected) rather than against a value
 * object of its own, because this context passes branch names through as raw strings. Without the
 * constraint a null branch reaches the git layer and becomes the literal ref {@code refs/heads/null}
 * (see {@code MergeGitAdapter:84}), which fails later and further from the cause.
 */
public record PullRequestCreateRequest(
        @NotBlank(message = "sourceBranch must not be blank") String sourceBranch,
        @NotBlank(message = "targetBranch must not be blank") String targetBranch) {
}
