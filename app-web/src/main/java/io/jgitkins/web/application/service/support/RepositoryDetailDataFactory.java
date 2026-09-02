package io.jgitkins.web.application.service.support;

import io.jgitkins.web.application.contract.BranchSummary;
import io.jgitkins.web.application.contract.RepositoryDetailData;
import io.jgitkins.web.application.contract.RepositoryFileEntry;
import io.jgitkins.web.application.contract.RepositoryOverviewResult;
import io.jgitkins.web.application.contract.RepositorySummary;
import io.jgitkins.web.application.translator.RepositoryDetailDataMapper;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.infrastructure.util.PathUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryDetailDataFactory {

	private final RepositoryDetailDataMapper repositoryDetailDataMapper;

	public RepositoryDetailData notFound() {
		return new RepositoryDetailData(null, List.of(), List.of(), null, null, null, null, "NONE", false,
				"Repository not found.");
	}

	public RepositoryDetailData pathMissing(RepositorySummary repository) {
		return new RepositoryDetailData(repository, List.of(), List.of(), null, null, null, null, "NONE", false,
				"Repository path is missing.");
	}

	public RepositoryDetailData fromOverview(RepositoryOverviewResult overview, RepositoryKey key) {
		RepositorySummary repository = overview.repository();
		String ownerSlug = PathUtils.lastSegment(key.namespace());
		List<BranchSummary> branches = overview.branches() == null ? List.of() : overview.branches();
		List<RepositoryFileEntry> files = overview.tree() == null ? List.of() : overview.tree();

		return repositoryDetailDataMapper.toDetail(
				repository,
				branches,
				files,
				key.namespace(),
				ownerSlug,
				key.repoName(),
				overview.selectedBranch(),
				overview.role(),
				overview.writable()
		);
	}

	public RepositoryDetailData withFiles(RepositoryDetailData baseDetail, List<RepositoryFileEntry> files) {
		List<RepositoryFileEntry> safeFiles = files == null ? List.of() : files;
		return new RepositoryDetailData(
				baseDetail.repository(),
				baseDetail.branches(),
				safeFiles,
				baseDetail.namespace(),
				baseDetail.ownerSlug(),
				baseDetail.repoName(),
				baseDetail.selectedBranch(),
				baseDetail.role(),
				baseDetail.writable(),
				baseDetail.errorMessage()
		);
	}
}
