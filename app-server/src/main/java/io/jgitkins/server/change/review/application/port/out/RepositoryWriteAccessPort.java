package io.jgitkins.server.change.review.application.port.out;

/**
 * May this requester write to that repository?
 *
 * <p>Merging writes to the target branch, so it answers to the same rule as committing. Stating the
 * rule again here would let the two drift, and the direction they drift in is whichever one a later
 * change forgets -- which for this rule means letting a stranger move someone else's branch.
 */
public interface RepositoryWriteAccessPort {

    /**
     * @throws io.jgitkins.server.shared.application.exception.ApplicationException 401 when the
     *     requester is absent, 403 when the repository is visible but not writable, 404 when it is
     *     not visible at all
     */
    void requireWriteAccess(String namespace, String repoName, Long requesterUserId);
}
