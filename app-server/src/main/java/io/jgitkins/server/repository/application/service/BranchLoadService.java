package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.contract.result.RepositoryKey;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchLoadService implements BranchLoadUseCase {

    private final BranchQueryPort branchQueryPort;
    private final RepositoryLoadUseCase repositoryLoadUseCase;
    private final RepositoryAccessValidator repositoryAccessValidator;

    /**
     * Turns an id into a name pair and authorizes it, in that order, before any branch is read.
     *
     * <p>The id-to-key lookup answers not-found for an id that does not resolve, and
     * {@code validateReadAccess} answers not-found for one the caller cannot see. Both are
     * {@link RepositoryNotFoundException}, so the two cases are indistinguishable from outside --
     * which is the point: a different answer for "exists but private" would be an existence oracle.
     */
    private void requireReadable(Long repositoryId, Long requesterUserId) {
        RepositoryKey key = repositoryLoadUseCase.resolveRepositoryKey(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
        repositoryAccessValidator.validateReadAccess(key.namespace(), key.repoName(), requesterUserId);
    }

    @Override
    public List<BranchSearchResult> loadBranches(Long repositoryId, Long requesterUserId) {
        requireReadable(repositoryId, requesterUserId);
        return branchQueryPort.findAllByRepositoryId(repositoryId);
    }

    @Override
    public BranchSearchResult loadBranch(Long repositoryId, String branchName, Long requesterUserId) {
        requireReadable(repositoryId, requesterUserId);
        return branchQueryPort.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new BranchNotFoundException(branchName));
    }
}
