package io.jgitkins.server.collaboration.application.port.out;

import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import java.util.Optional;

public interface OrganizeMembershipQueryPort {
    Optional<OrganizeMemberRole> findRoleByOrganizeIdAndUserId(Long organizeId, Long userId);
    long countOwnersByOrganizeId(Long organizeId);
}
