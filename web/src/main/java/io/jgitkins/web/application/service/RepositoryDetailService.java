package io.jgitkins.web.application.service;

import io.jgitkins.web.application.dto.RepositoryDetailData;
import io.jgitkins.web.application.dto.RepositoryFileEntry;
import io.jgitkins.web.application.dto.RepositoryFileIndexEntry;
import io.jgitkins.web.application.dto.RepositoryOverviewResult;
import io.jgitkins.web.application.dto.RepositorySummary;
import io.jgitkins.web.application.model.RepositoryKey;
import io.jgitkins.web.application.port.in.RepositoryDetailUseCase;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.application.service.support.RepositoryDetailDataFactory;
import io.jgitkins.web.application.service.support.RepositoryFileSearchPolicy;
import io.jgitkins.web.application.service.support.RepositoryKeyResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RepositoryDetailService implements RepositoryDetailUseCase {

	private final RepositoryPort repositoryPort;
	private final RepositoryDetailDataFactory repositoryDetailDataFactory;
	private final RepositoryFileSearchPolicy repositoryFileSearchPolicy;
	private final RepositoryKeyResolver repositoryKeyResolver;

	@Override
	public RepositoryDetailData loadRepositoryDetail(Long repositoryId, String branch) {
		RepositoryOverviewResult overview = repositoryPort.fetchRepositoryOverview(repositoryId, branch);
		return buildDetail(overview);
	}

	@Override
	public RepositoryDetailData loadRepositoryByPath(String namespace, String repoName, String branch, String directory) {
		RepositoryOverviewResult overview = repositoryPort.fetchRepositoryOverviewByPath(namespace, repoName, branch);
		RepositoryDetailData baseDetail = buildDetail(overview);
		if (baseDetail.repository() == null) {
			return baseDetail;
		}
		String selectedBranch = baseDetail.selectedBranch();
		String normalizedDirectory = StringUtils.hasText(directory) ? directory.trim() : "";
		List<RepositoryFileEntry> files = repositoryPort.fetchRepositoryTree(namespace, repoName, selectedBranch, normalizedDirectory);
		return repositoryDetailDataFactory.withFiles(baseDetail, files);
	}

	private RepositoryDetailData buildDetail(RepositoryOverviewResult overview) {
		if (overview == null || overview.repository() == null) {
			return repositoryDetailDataFactory.notFound();
		}
		RepositorySummary repository = overview.repository();
		RepositoryKey key = repositoryKeyResolver.resolve(repository);
		if (key == null) {
			return repositoryDetailDataFactory.pathMissing(repository);
		}
		return repositoryDetailDataFactory.fromOverview(overview, key);
	}

	@Override
	public List<RepositoryFileIndexEntry> loadRepositoryFileIndexByPath(String namespace, String repoName, String branch) {
		String selectedBranch = StringUtils.hasText(branch) ? branch.trim() : "main";
		return repositoryPort.fetchRepositoryFileIndex(namespace, repoName, selectedBranch);
	}

	@Override
	public List<RepositoryFileIndexEntry> searchRepositoryFilesByPath(String namespace, String repoName, String branch, String query, int limit) {
		List<RepositoryFileIndexEntry> index = loadRepositoryFileIndexByPath(namespace, repoName, branch);
		return repositoryFileSearchPolicy.search(index, query, limit);
	}
}
