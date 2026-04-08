package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.exception.OrganizeMemberAlreadyExistsException;
import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.domain.model.vo.OrganizeId;
import io.jgitkins.server.domain.model.vo.OrganizeMemberRole;
import io.jgitkins.server.domain.model.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeMemberValidator {

    private final OrganizeMemberPersistencePort organizeMemberPort;

    public OrganizeMemberRole resolveRole(OrganizeMemberRole role) {
        return role != null ? role : OrganizeMemberRole.MEMBER;
    }

    public void validateMemberNotExists(OrganizeId organizeId, UserId userId) {
        if (organizeMemberPort.existsByOrganizeIdAndUserId(organizeId, userId)) {
            throw new OrganizeMemberAlreadyExistsException(organizeId.getValue(), userId.getValue());
        }
    }
}
