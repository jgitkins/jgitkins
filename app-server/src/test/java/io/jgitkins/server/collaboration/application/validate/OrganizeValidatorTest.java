package io.jgitkins.server.collaboration.application.validate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeValidatorTest {
    @Mock OrganizeMembershipQueryPort organizeMembershipQueryPort;

    private final Organize organize = Organize.reconstruct(
            OrganizeId.of(10L), OrganizeName.from("team"), "description", OwnerId.of(7L),
            LocalDateTime.now(), LocalDateTime.now());

    @Test
    void ownerIsAccessibleWithoutMembershipLookup() {
        assertTrue(new OrganizeValidator(null, organizeMembershipQueryPort).isAccessible(organize, 7L));
        verify(organizeMembershipQueryPort, never()).findRoleByOrganizeIdAndUserId(10L, 7L);
    }

    @Test
    void memberIsAccessibleByNumericUserId() {
        when(organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(10L, 8L))
                .thenReturn(Optional.of(OrganizeMemberRole.MEMBER));

        assertTrue(new OrganizeValidator(null, organizeMembershipQueryPort).isAccessible(organize, 8L));
    }

    @Test
    void membershipDenialIsFalse() {
        when(organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(10L, 8L))
                .thenReturn(Optional.empty());

        assertFalse(new OrganizeValidator(null, organizeMembershipQueryPort).isAccessible(organize, 8L));
    }

    @Test
    void nullOrganizeOrIdIsDenied() {
        OrganizeValidator validator = new OrganizeValidator(null, organizeMembershipQueryPort);

        assertFalse(validator.isAccessible(null, 7L));
        assertFalse(validator.isAccessible(organizeWithoutId(), 7L));
    }

    @Test
    void nullCurrentUserIsDenied() {
        assertFalse(new OrganizeValidator(null, organizeMembershipQueryPort).isAccessible(organize, null));
    }

    private Organize organizeWithoutId() {
        return Organize.createWithoutEvent(null, OrganizeName.from("without-id"), OwnerId.of(7L),
                "description", LocalDateTime.now());
    }
}
