package io.jgitkins.server.domain.repository;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.RepositoryId;

public interface RepositoryRepository {

    Repository save(Repository repository);

    Repository update(Repository repository);

    void deleteById(RepositoryId id);
}
