package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import java.util.List;

public record RepositoryOverviewResult(
        RepositoryResult repository,
        List<BranchSearchResult> branches,
        List<FileEntry> tree,
        String selectedBranch,
        String role,
        boolean writable
) {
}
