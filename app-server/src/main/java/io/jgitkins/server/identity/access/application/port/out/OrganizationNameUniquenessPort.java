package io.jgitkins.server.identity.access.application.port.out;

public interface OrganizationNameUniquenessPort {
    boolean isAvailableForUsername(String username);
}
