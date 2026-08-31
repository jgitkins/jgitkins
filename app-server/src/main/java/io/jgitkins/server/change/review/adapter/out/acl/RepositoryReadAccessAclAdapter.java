package io.jgitkins.server.change.review.adapter.out.acl;

import io.jgitkins.server.change.review.application.port.out.RepositoryReadAccessPort;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Delegates to the repository context's own read gate rather than restating it.
 *
 * <p>Mirrors {@link RepositoryWriteAccessAclAdapter}. {@code validateReadAccess} already carries the
 * rule task 2.125 established: {@code canRead} checks PUBLIC before membership, so an anonymous read
 * of a public repository succeeds, and anything the caller cannot see answers not-found.
 * Reimplementing that here would produce a second copy of a security rule.
 */
@Component
@RequiredArgsConstructor
public class RepositoryReadAccessAclAdapter implements RepositoryReadAccessPort {

    private final RepositoryAccessValidator repositoryAccessValidator;

    @Override
    public void requireReadAccess(String namespace, String repoName, Long requesterUserId) {
        repositoryAccessValidator.validateReadAccess(namespace, repoName, requesterUserId);
    }
}
