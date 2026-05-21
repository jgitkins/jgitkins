package io.jgitkins.server.shared.application.support;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import io.jgitkins.server.identity.access.domain.vo.UserId;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryAccessibilityService {

    private final OrganizeMembershipQueryPort organizeMemberPort;

    public boolean isVisibleToRequester(
            Repository repository,
            Optional<Long> requesterId,
            Map<OrganizeId, Boolean> membershipCache) {
        if (repository == null) {
            return false;
        }
        if (repository.getVisibility() == RepositoryVisibility.PUBLIC) {
            return true;
        }
        if (requesterId.isEmpty()) {
            return false;
        }

        Long userId = requesterId.get();
        if (repository.getOwnerType() == OwnerType.USER) {
            return repository.getOwnerId() != null
                    && userId.equals(repository.getOwnerId().getValue());
        }
        if (repository.getOwnerType() == OwnerType.ORGANIZATION && repository.getOwnerId() != null) {
            OrganizeId organizeId = OrganizeId.of(repository.getOwnerId().getValue());
            return membershipCache.computeIfAbsent(
                    organizeId,
                    id -> organizeMemberPort.existsByOrganizeIdAndUserId(id, UserId.of(userId)));
        }
        return false;
    }

    public boolean isVisibleToUserOwner(
            Repository repository,
            Optional<Long> requesterId,
            Long ownerId) {
        if (repository == null) {
            return false;
        }
        if (repository.getVisibility() == RepositoryVisibility.PUBLIC) {
            return true;
        }
        return requesterId.isPresent()
                && ownerId != null
                && ownerId.equals(requesterId.get());
    }
}
