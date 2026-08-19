package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import java.util.Optional;

public interface OrganizationMembershipPort {
    Optional<OrganizationMembershipRole> findRoleByOrganizationIdAndUserId(Long organizationId, Long userId);
}
