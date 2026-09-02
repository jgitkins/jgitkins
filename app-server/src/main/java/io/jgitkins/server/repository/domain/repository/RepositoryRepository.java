package io.jgitkins.server.repository.domain.repository;

import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.RepositoryOwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.domain.vo.RepositoryId;
import io.jgitkins.server.repository.domain.vo.RepositoryName;
import io.jgitkins.server.repository.domain.vo.RepositoryPath;
import java.util.Optional;

public interface RepositoryRepository {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);

    Optional<Repository> findById(RepositoryId id);

    Optional<Repository> findByOwnerAndName(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryName name);

    Optional<Repository> findByOwnerAndPath(OwnerType ownerType, RepositoryOwnerId ownerId, RepositoryPath path);

    Optional<Repository> findByClonePath(String clonePath);

    Optional<Repository> findByPath(String path);
}
