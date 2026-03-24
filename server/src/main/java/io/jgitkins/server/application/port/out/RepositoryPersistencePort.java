package io.jgitkins.server.application.port.out;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.*;

import java.util.List;
import java.util.Optional;

public interface RepositoryPersistencePort {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);

    Optional<Repository> findById(RepositoryId id);

    List<Repository> findAll();

    Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path);
    Optional<Repository> findByClonePath(String clonePath);

    Optional<Repository> findByPath(String path);

    Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name);

    Optional<Long> findIdByOwnerAndName(OwnerType ownerType, OwnerId ownerId, String repoName);

    List<Repository> findAllByOwner(OwnerType ownerType, OwnerId ownerId);

    long countByOwner(OwnerType ownerType, OwnerId ownerId);

}
