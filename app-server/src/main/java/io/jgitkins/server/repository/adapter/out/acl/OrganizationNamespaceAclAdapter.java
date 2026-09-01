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
 * <p>The validity check is the contract, not a precaution. {@code OrganizeName.from} throws on
 * anything outside {@code [A-Za-z0-9_-]+}, and the argument here is a path segment a client typed, so
 * without the check every unusable namespace becomes a 500 where the caller wanted a 404.
 * {@code OrganizeName.isValid} exists for exactly this: asking whether a string could be a name,
 * without constructing one.
 *
 * <p>Answering it here rather than in each caller is the point. Before this, only
 * {@code RepositoryLookupService} knew to catch the exception, and the two persistence adapters that
 * needed the same lookup went around the port to collaboration's mapper instead -- which is how the
 * guard stayed in one place by keeping the port unused.
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
