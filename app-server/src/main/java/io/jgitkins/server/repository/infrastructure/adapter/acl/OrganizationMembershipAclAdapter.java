package io.jgitkins.server.repository.infrastructure.adapter.acl;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationMembershipAclAdapter implements OrganizationMembershipPort {
    private final OrganizeMembershipQueryPort delegate;
    @Override public Optional<OrganizationMembershipRole> findRoleByOrganizationIdAndUserId(Long organizationId, Long userId) {
        return delegate.findRoleByOrganizeIdAndUserId(organizationId, userId).map(this::map);
    }
    private OrganizationMembershipRole map(OrganizeMemberRole role) {
        return switch (role) {
            case OWNER -> OrganizationMembershipRole.OWNER;
            case MAINTAINER -> OrganizationMembershipRole.MAINTAINER;
            case MEMBER -> OrganizationMembershipRole.MEMBER;
        };
    }
}
