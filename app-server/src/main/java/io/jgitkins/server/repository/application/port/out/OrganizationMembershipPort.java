package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import java.util.List;
import java.util.Optional;

public interface OrganizationMembershipPort {
    Optional<OrganizationMembershipRole> findRoleByOrganizationIdAndUserId(Long organizationId, Long userId);

    /**
     * The organizations this user belongs to, by id, for the visibility filter on a repository list.
     *
     * <p>Ids only. This context filters repositories by owner id and has no use for a role or a
     * membership object, so asking for less keeps the boundary narrow.
     *
     * <p>Empty for a null user and for a user in no organization -- neither is an error, and a
     * repository list for such a caller simply contains no organization-owned rows.
     */
    List<Long> findOrganizationIdsByUserId(Long userId);
}
