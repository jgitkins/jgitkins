package io.jgitkins.server.repository.adapter.out.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jgitkins.server.collaboration.application.port.out.OrganizeMembershipQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.repository.domain.vo.OrganizationMembershipRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrganizationMembershipAclAdapterTest {
    @Test
    void mapsForeignMembershipRolesAndPreservesEmptyResult() {
        OrganizeMembershipQueryPort delegate = mock(OrganizeMembershipQueryPort.class);
        when(delegate.findRoleByOrganizeIdAndUserId(1L, 2L)).thenReturn(Optional.of(OrganizeMemberRole.MAINTAINER));
        OrganizationMembershipAclAdapter adapter = new OrganizationMembershipAclAdapter(delegate);

        assertThat(adapter.findRoleByOrganizationIdAndUserId(1L, 2L))
                .contains(OrganizationMembershipRole.MAINTAINER);

        when(delegate.findRoleByOrganizeIdAndUserId(1L, 3L)).thenReturn(Optional.empty());
        assertThat(adapter.findRoleByOrganizationIdAndUserId(1L, 3L)).isEmpty();
    }
}
