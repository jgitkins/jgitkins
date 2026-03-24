package io.jgitkins.server.application.validate;

import io.jgitkins.server.application.port.out.OrganizeMemberPersistencePort;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.domain.error.DomainErrorCode;
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
            throw new ApplicationException(DomainErrorCode.ORGANIZE_MEMBER_ALREADY_EXISTS,
                    String.format("Organize member already exists: organizeId=%s, userId=%s",
                            organizeId.getValue(), userId.getValue()));
        }
    }
}
