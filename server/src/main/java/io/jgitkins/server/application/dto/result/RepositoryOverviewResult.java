package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.application.dto.FileEntry;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepositoryOverviewResult {

	private final RepositoryResult repository;
	private final List<BranchSearchResult> branches;
	private final List<FileEntry> tree;
	private final String selectedBranch;
	private final String role;
	private final boolean writable;
}
