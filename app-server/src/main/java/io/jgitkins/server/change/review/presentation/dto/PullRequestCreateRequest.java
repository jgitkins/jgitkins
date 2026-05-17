package io.jgitkins.server.change.review.presentation.dto;

public record PullRequestCreateRequest(String sourceBranch, String targetBranch) {
}
