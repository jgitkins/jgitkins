package io.jgitkins.server.identity.access.application.port.out;

public interface OwnedRepositoryCountPort {
    long countByUserId(Long userId);
}
