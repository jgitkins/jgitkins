package io.jgitkins.server.change.review.adapter.out.acl;

import io.jgitkins.server.change.review.application.port.out.RepositoryWriteAccessPort;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Delegates to the repository context's own write gate rather than restating it.
 *
 * <p>{@code validateCanCommit} already carries the visibility split added in 577c1a0: a repository
 * the caller cannot see answers not-found, one they can see but may not write answers forbidden.
 * Reimplementing that here would produce a second copy of a security rule.
 *
 * <p>Depends on the concrete validator, matching {@code RepositoryReferenceAclAdapter}, which
 * depends on {@code RepositoryLookupService} the same way.
 */
@Component
@RequiredArgsConstructor
public class RepositoryWriteAccessAclAdapter implements RepositoryWriteAccessPort {

    private final RepositoryAccessValidator repositoryAccessValidator;

    @Override
    public void requireWriteAccess(String namespace, String repoName, Long requesterUserId) {
        repositoryAccessValidator.validateCanCommit(namespace, repoName, requesterUserId);
    }
}
