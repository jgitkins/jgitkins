package io.jgitkins.server.repository.application.service.internal;

import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.repository.domain.aggregate.Repository;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.application.contract.RepositoryPermission;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GitRepositoryAccessUseCaseFacade implements GitRepositoryAccessUseCase {

    private final GitRepositoryAccessService gitRepositoryAccessService;

    @Override
    public boolean canRead(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return gitRepositoryAccessService.canRead(ownerType, ownerName, repositoryName, userId);
    }

    @Override
    public boolean canWrite(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return gitRepositoryAccessService.canWrite(ownerType, ownerName, repositoryName, userId);
    }

    @Override
    public RepositoryPermission resolvePermission(OwnerType ownerType, String ownerName, String repositoryName, Long userId) {
        return gitRepositoryAccessService.resolvePermission(ownerType, ownerName, repositoryName, userId);
    }

    @Override
    public RepositoryPermission resolvePermission(Repository repo, Long userId) {
        return gitRepositoryAccessService.resolvePermission(repo, userId);
    }

    @Override
    public RepositoryPermission resolvePermission(RepositoryResult repo, Long userId) {
        return gitRepositoryAccessService.resolvePermission(repo, userId);
    }

    @Override
    public Optional<Boolean> resolveVisibility(OwnerType ownerType, String ownerName, String repositoryName) {
        return gitRepositoryAccessService.resolveVisibility(ownerType, ownerName, repositoryName);
    }
}
