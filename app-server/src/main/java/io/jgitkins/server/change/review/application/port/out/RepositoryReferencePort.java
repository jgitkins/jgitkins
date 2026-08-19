package io.jgitkins.server.change.review.application.port.out;

import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import java.util.Optional;

public interface RepositoryReferencePort {
    Optional<ReviewRepositoryReference> findByPath(String namespace, String repoName);
    Optional<ReviewRepositoryReference> findById(ReviewRepositoryId repositoryId);
}
