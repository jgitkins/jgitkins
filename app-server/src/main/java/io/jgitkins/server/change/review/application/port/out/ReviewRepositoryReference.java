package io.jgitkins.server.change.review.application.port.out;

import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;

public record ReviewRepositoryReference(ReviewRepositoryId id, String namespace, String repoName) {
    public ReviewRepositoryReference {
        if (id == null || namespace == null || namespace.isBlank() || repoName == null || repoName.isBlank()) throw new IllegalArgumentException("Review repository reference is invalid");
    }
}
