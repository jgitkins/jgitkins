package io.jgitkins.server.repository.application.port.in;

import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import java.util.Optional;

public interface GitRepositoryAccessUseCase {

    boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    RepositoryPermission resolvePermission(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    RepositoryPermission resolvePermission(Repository repo, Long userId);

    Optional<Boolean> resolveVisibility(OwnerType ownerType, String ownerName, String repositoryName);
}
