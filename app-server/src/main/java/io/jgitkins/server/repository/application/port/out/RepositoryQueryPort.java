package io.jgitkins.server.repository.application.port.out;

import io.jgitkins.server.shared.domain.model.vo.OwnerId;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import java.util.List;
import java.util.Optional;

public interface RepositoryQueryPort {

    Optional<RepositoryResult> loadRepository(Long repositoryId);

    Optional<RepositoryResult> loadRepositoryByPath(String namespace, String repoName);

    List<RepositoryResult> loadVisibleRepositories(Long requesterId);

    List<RepositoryResult> loadUserRepositories(String username, Long requesterId);

    long countByOwner(OwnerType ownerType, OwnerId ownerId);
}
