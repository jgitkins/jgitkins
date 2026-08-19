package io.jgitkins.server.repository.infrastructure.adapter.acl;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationNamespaceAclAdapter implements OrganizationNamespacePort {
    private final OrganizeQueryPort delegate;
    @Override public Optional<Long> findOrganizationIdByName(String organizationName) {
        return delegate.findByName(OrganizeName.from(organizationName)).map(o -> o.getId().getValue());
    }
}
