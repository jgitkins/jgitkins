package io.jgitkins.server.repository.application.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.repository.domain.vo.RepositoryVisibility;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryAccessibilityServiceTest {
    @Mock OrganizationMembershipPort organizationMembershipPort;
    @Mock Repository repository;

    @Test
    void organizationMembershipCacheUsesNumericOrganizationId() {
        RepositoryAccessibilityService service = new RepositoryAccessibilityService(organizationMembershipPort);
        when(repository.getVisibility()).thenReturn(RepositoryVisibility.PRIVATE);
        when(repository.getOwnerType()).thenReturn(OwnerType.ORGANIZATION);
        when(repository.getOwnerId()).thenReturn(RepositoryOwnerId.of(10L));
        when(organizationMembershipPort.findRoleByOrganizationIdAndUserId(10L, 7L))
                .thenReturn(Optional.empty());
        HashMap<Long, Boolean> cache = new HashMap<>();

        assertFalse(service.isVisibleToRequester(repository, Optional.of(7L), cache));
        assertFalse(cache.get(10L));
    }

    @Test
    void publicRepositoryIsVisibleWithoutRequester() {
        RepositoryAccessibilityService service = new RepositoryAccessibilityService(organizationMembershipPort);
        when(repository.getVisibility()).thenReturn(RepositoryVisibility.PUBLIC);

        assertTrue(service.isVisibleToRequester(repository, Optional.empty(), new HashMap<>()));
    }
}
