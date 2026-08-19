package io.jgitkins.server.identity.access.infrastructure.adapter.acl;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.application.port.out.OrganizationNameUniquenessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationNameUniquenessAclAdapter implements OrganizationNameUniquenessPort {
    private final OrganizeQueryPort organizeQueryPort;

    @Override
    public boolean isAvailableForUsername(String username) {
        if (!OrganizeName.isValid(username)) return true;
        try {
            return organizeQueryPort.findByName(OrganizeName.from(username)).isEmpty();
        } catch (JgitkinsException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Organization namespace availability query failed", e);
        }
    }
}
