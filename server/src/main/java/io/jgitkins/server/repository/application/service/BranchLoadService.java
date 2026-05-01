package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.exception.BranchNotFoundException;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchLoadService implements BranchLoadUseCase {

    private final BranchQueryPort branchQueryPort;

    @Override
    public List<BranchSearchResult> loadBranches(Long repositoryId) {
        return branchQueryPort.findAllByRepositoryId(repositoryId);
    }

    @Override
    public BranchSearchResult loadBranch(Long repositoryId, String branchName) {
        return branchQueryPort.findByRepositoryIdAndName(repositoryId, branchName)
                .orElseThrow(() -> new BranchNotFoundException(branchName));
    }
}
