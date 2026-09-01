package io.jgitkins.server.identity.access.adapter.out.acl;

import io.jgitkins.core.common.exception.JgitkinsException;
import io.jgitkins.server.common.infrastructure.error.InfrastructureErrorCode;
import io.jgitkins.server.common.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.identity.access.application.port.out.OwnedRepositoryCountPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnedRepositoryCountAclAdapter implements OwnedRepositoryCountPort {
    private final RepositoryQueryPort repositoryQueryPort;

    @Override
    public long countByUserId(Long userId) {
        try {
            return repositoryQueryPort.countByOwner(OwnerType.USER, RepositoryOwnerId.of(userId));
        } catch (JgitkinsException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(InfrastructureErrorCode.PERSISTENCE_OPERATION_FAILED,
                    "Owned repository count query failed", e);
        }
    }
}
