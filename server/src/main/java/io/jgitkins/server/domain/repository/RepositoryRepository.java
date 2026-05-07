package io.jgitkins.server.domain.repository;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerId;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.domain.model.vo.RepositoryId;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import io.jgitkins.server.domain.model.vo.RepositoryPath;
import java.util.Optional;

public interface RepositoryRepository {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);

    Optional<Repository> findById(RepositoryId id);

    Optional<Repository> findByOwnerAndName(OwnerType ownerType, OwnerId ownerId, RepositoryName name);

    Optional<Repository> findByOwnerAndPath(OwnerType ownerType, OwnerId ownerId, RepositoryPath path);

    Optional<Repository> findByClonePath(String clonePath);

    Optional<Repository> findByPath(String path);
}
