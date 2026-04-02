package io.jgitkins.server.application.service;

import io.jgitkins.server.application.common.error.ApplicationErrorCode;
import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.application.port.out.BranchQueryPort;
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
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.BRANCH_NOT_FOUND, "Branch not found: " + branchName));
    }
}
