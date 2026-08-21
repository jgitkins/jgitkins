package io.jgitkins.server.collaboration.application.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.collaboration.domain.vo.MemberUserId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizeMemberValidatorTest {
    @Mock OrganizeMembershipQueryPort organizeMembershipQueryPort;

    @Test
    void defaultsMissingRoleToMember() {
        OrganizeMemberValidator validator = new OrganizeMemberValidator(organizeMembershipQueryPort);
        assertEquals(OrganizeMemberRole.MEMBER, validator.resolveRole(null));
    }

    @Test
    void resolvesExistingMemberRoleThroughOwnedPort() {
        when(organizeMembershipQueryPort.findRoleByOrganizeIdAndUserId(10L, 8L))
                .thenReturn(Optional.empty());
        OrganizeMemberValidator validator = new OrganizeMemberValidator(organizeMembershipQueryPort);

        assertDoesNotThrow(() -> validator.validateMemberNotExists(OrganizeId.of(10L), MemberUserId.of(8L)));
    }
}
