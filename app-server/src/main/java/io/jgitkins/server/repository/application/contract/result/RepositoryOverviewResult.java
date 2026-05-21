package io.jgitkins.server.repository.application.contract.result;

import io.jgitkins.server.repository.application.contract.result.FileEntry;
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
