package io.jgitkins.server.collaboration.application.validate;

import io.jgitkins.server.collaboration.application.exception.OrganizeMemberAlreadyExistsException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeMemberValidator {

    private final OrganizeMembershipQueryPort organizeMemberPort;

    public OrganizeMemberRole resolveRole(OrganizeMemberRole role) {
        return role != null ? role : OrganizeMemberRole.MEMBER;
    }

    public void validateMemberNotExists(OrganizeId organizeId, MemberUserId userId) {
        if (organizeMemberPort.findRoleByOrganizeIdAndUserId(organizeId.getValue(), userId.getValue()).isPresent()) {
            throw new OrganizeMemberAlreadyExistsException(organizeId.getValue(), userId.getValue());
        }
    }
}
