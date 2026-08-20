package io.jgitkins.server.repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.repository.application.port.out.OrganizationMembershipPort;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.application.port.out.UserNamespacePort;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationMembershipAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.OrganizationNamespaceAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.RepositoryActorAclAdapter;
import io.jgitkins.server.repository.adapter.out.acl.UserNamespaceAclAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

class RepositoryAclAdapterWiringTest {
    @Test
    void aclAdaptersHaveUniqueComponentsAndImplementRepositoryPorts() {
        assertThat(RepositoryActorPort.class).isAssignableFrom(RepositoryActorAclAdapter.class);
        assertThat(UserNamespacePort.class).isAssignableFrom(UserNamespaceAclAdapter.class);
        assertThat(OrganizationNamespacePort.class).isAssignableFrom(OrganizationNamespaceAclAdapter.class);
        assertThat(OrganizationMembershipPort.class).isAssignableFrom(OrganizationMembershipAclAdapter.class);
        assertThat(RepositoryActorAclAdapter.class).hasAnnotation(Component.class);
        assertThat(UserNamespaceAclAdapter.class).hasAnnotation(Component.class);
        assertThat(OrganizationNamespaceAclAdapter.class).hasAnnotation(Component.class);
        assertThat(OrganizationMembershipAclAdapter.class).hasAnnotation(Component.class);
    }
}
