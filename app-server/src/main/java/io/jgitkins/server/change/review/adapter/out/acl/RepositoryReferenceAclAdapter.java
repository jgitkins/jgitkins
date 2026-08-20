package io.jgitkins.server.change.review.adapter.out.acl;

import io.jgitkins.server.change.review.application.port.out.RepositoryReferencePort;
import io.jgitkins.server.change.review.application.port.out.ReviewRepositoryReference;
import io.jgitkins.server.change.review.domain.model.vo.ReviewRepositoryId;
import io.jgitkins.server.repository.application.support.RepositoryLookupService;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.shared.application.support.RepositoryNamespaceResolver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryReferenceAclAdapter implements RepositoryReferencePort {
    private final RepositoryLookupService repositoryLookupService;
    private final RepositoryRepository repositoryRepository;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;
    @Override public Optional<ReviewRepositoryReference> findByPath(String namespace, String repoName) {
        return repositoryLookupService.resolveByPath(namespace, repoName).map(this::map);
    }
    @Override public Optional<ReviewRepositoryReference> findById(ReviewRepositoryId repositoryId) {
        return repositoryRepository.findById(RepositoryId.of(repositoryId.value())).map(this::map);
    }
    private ReviewRepositoryReference map(Repository repository) {
        return new ReviewRepositoryReference(ReviewRepositoryId.of(repository.getId().getValue()), repositoryNamespaceResolver.resolve(repository), repository.getPath().getValue());
    }
}
