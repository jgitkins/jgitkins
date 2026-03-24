package io.jgitkins.server.application.port.in;

import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.model.vo.OwnerType;
import java.util.Optional;

public interface GitRepositoryAccessUseCase {

    boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    RepositoryPermission resolvePermission(OwnerType ownerType, String ownerName, String repositoryName, Long userId);

    RepositoryPermission resolvePermission(Repository repo, Long userId);

    Optional<Boolean> resolveVisibility(OwnerType ownerType, String ownerName, String repositoryName);

    record RepositoryPermission(String role, boolean writable, boolean member) {
        public static RepositoryPermission anonymous() {
            return new RepositoryPermission("ANONYMOUS", false, false);
        }

        public static RepositoryPermission none() {
            return new RepositoryPermission("NONE", false, false);
        }
    }
}
