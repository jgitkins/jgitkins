package io.jgitkins.server.change.review.adapter.in.rest.dto.request;

public record PullRequestCreateRequest(String sourceBranch, String targetBranch) {
}
