package io.jgitkins.server.collaboration.application.port.out;

/**
 * How many repositories an organization owns.
 *
 * <p>Mirrors {@code OwnedRepositoryCountPort} in identity-access, which asks the same question about
 * a user. Narrow on purpose: collaboration needs a count to decide whether deletion would orphan
 * anything, not a view of the repository context.
 */
public interface OrganizeOwnedRepositoryCountPort {
    long countByOrganizeId(Long organizeId);
}
