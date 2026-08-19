package io.jgitkins.server.repository.application.port.out;

import java.util.Optional;

public interface OrganizationNamespacePort {
    Optional<Long> findOrganizationIdByName(String organizationName);
}
