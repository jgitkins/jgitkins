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
     * argument is a namespace out of a URL, so throwing would turn a 404 into a 500 for any path a
     * client can type. Stated here because the implementation builds a validating value object whose
     * constructor does throw -- deciding it once keeps the guard out of every caller.
     */
    Optional<Long> findOrganizationIdByName(String organizationName);
}
