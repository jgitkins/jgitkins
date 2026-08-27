package io.jgitkins.server.repository.adapter.out.persistence;

import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.domain.repository.RepositoryRepository;

/**
 * The two repository-context persistence ports as one type.
 *
 * <p>Both existing adapters implement both ports, and the selector has to hand back a single object
 * for both bean definitions or the two ports could be served by different providers. Naming the pair
 * makes that a compile-time fact rather than a convention the configuration has to remember.
 */
public interface RepositoryPersistence extends RepositoryRepository, RepositoryQueryPort {
}
