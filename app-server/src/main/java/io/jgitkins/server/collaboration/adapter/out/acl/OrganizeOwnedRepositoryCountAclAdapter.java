package io.jgitkins.server.collaboration.adapter.out.acl;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.collaboration.application.port.out.OrganizeOwnedRepositoryCountPort;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeOwnedRepositoryCountAclAdapter implements OrganizeOwnedRepositoryCountPort {

    private final RepositoryQueryPort repositoryQueryPort;

    @Override
    public long countByOrganizeId(Long organizeId) {
        try {
            return repositoryQueryPort.countByOwner(OwnerType.ORGANIZATION, RepositoryOwnerId.of(organizeId));
        } catch (JgitkinsException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Organization owned repository count query failed", e);
        }
    }
}
