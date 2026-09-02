package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.application.contract.RepositoryPermission;
import java.util.Optional;

public interface GitRepositoryAccessUseCase {

    boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    RepositoryPermission resolvePermission(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    RepositoryPermission resolvePermission(Repository repo, Long userId);

    /** Read-model overload, added by task 2.65 so read authorization needs no aggregate load. */
    RepositoryPermission resolvePermission(RepositoryResult repo, Long userId);

    Optional<Boolean> resolveVisibility(OwnerType ownerType, String ownerName, String repositoryName);
}
