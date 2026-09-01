package io.jgitkins.server.repository.adapter.out.acl;

import io.jgitkins.server.collaboration.application.port.out.OrganizeQueryPort;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.repository.application.port.out.OrganizationNamespacePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Translates a URL namespace into a collaboration organization id.
 *
 * <p>The validity check is the contract, not a precaution: {@code OrganizeName.from} throws outside
 * {@code [A-Za-z0-9_-]+} and the argument is a path segment a client typed, so without it an unusable
 * namespace is a 500 rather than a 404. Pinned by {@code OrganizationNamespaceAclAdapterTest}.
 */
@Component
@RequiredArgsConstructor
public class OrganizationNamespaceAclAdapter implements OrganizationNamespacePort {
    private final OrganizeQueryPort delegate;
    @Override public Optional<Long> findOrganizationIdByName(String organizationName) {
        if (!OrganizeName.isValid(organizationName)) {
            return Optional.empty();
        }
        return delegate.findByName(OrganizeName.from(organizationName)).map(o -> o.getId().getValue());
    }
}
