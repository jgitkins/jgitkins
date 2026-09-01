package io.jgitkins.server.repository.application.port.out;

import java.util.Optional;

/**
 * Resolves an organization name to its id, for callers that only have a URL segment.
 */
public interface OrganizationNamespacePort {

    /**
     * The organization's id, or empty.
     *
     * <p><strong>A name that cannot be an organization name is empty, not an exception.</strong> The
     * argument is a namespace out of a URL, so "not a legal name" and "no such organization" are the
     * same answer to the caller: there is no organization there. Throwing would turn a 404 into a 500
     * for any path a client can type, which is the shape 529ff34 had to fix elsewhere.
     *
     * <p>This is written down because the implementation builds a validating value object, and that
     * object's constructor does throw. Deciding the contract here rather than at each call site is
     * what keeps the guard from being copied into every caller and drifting.
     */
    Optional<Long> findOrganizationIdByName(String organizationName);
}
