package io.jgitkins.server.repository.application.service.internal;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryAccessibilityService {

    private final OrganizationMembershipPort organizationMembershipPort;

    public boolean isVisibleToRequester(
            Repository repository,
            Optional<Long> requesterId,
            Map<Long, Boolean> membershipCache) {
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
            Long organizationId = repository.getOwnerId().getValue();
            return membershipCache.computeIfAbsent(
                    organizationId,
                    id -> organizationMembershipPort
                            .findRoleByOrganizationIdAndUserId(id, userId)
                            .isPresent());
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
