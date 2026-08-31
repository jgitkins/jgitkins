package io.jgitkins.server.change.review.application.port.out;

/**
 * May this requester see that repository at all?
 *
 * <p>Pull requests and merge checks describe a repository's branches and diffs, so reading one is
 * reading the repository. Stating the visibility rule again in this context would let the two copies
 * drift, and the direction they drift in is whichever one a later change forgets.
 *
 * <p>The requester is nullable on purpose: a public repository is readable anonymously, and the
 * repository context's rule checks PUBLIC before membership. Demanding a requester here would answer
 * 401 to a logged-out visitor reading a public repository's pull request.
 */
public interface RepositoryReadAccessPort {

    /**
     * @param requesterUserId nullable — anonymous is a legitimate reader of a public repository
     * @throws RuntimeException the repository context's not-found exception, which maps to 404.
     *     Not-found rather than forbidden: a read denial means "cannot see it" by definition, and 403
     *     would confirm from the status alone that a private repository with that name exists. The
     *     concrete type is deliberately not named here — this interface is the boundary, and the
     *     architecture guard treats a foreign type in a port as the same violation whether it appears
     *     in code or in a doc comment.
     */
    void requireReadAccess(String namespace, String repoName, Long requesterUserId);
}
