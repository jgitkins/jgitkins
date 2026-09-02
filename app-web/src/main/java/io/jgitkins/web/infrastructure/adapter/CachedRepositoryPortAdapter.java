package io.jgitkins.web.infrastructure.adapter;

import io.jgitkins.web.application.contract.BranchSummary;
import io.jgitkins.web.application.contract.CommitSummary;
import io.jgitkins.web.application.contract.RepositoryBranchCreateResult;
import io.jgitkins.web.application.contract.RepositoryCreateRequest;
import io.jgitkins.web.application.contract.RepositoryCreateResult;
import io.jgitkins.web.application.contract.RepositoryFileEntry;
import io.jgitkins.web.application.contract.RepositoryFileIndexEntry;
import io.jgitkins.web.application.contract.RepositoryFileUploadRequest;
import io.jgitkins.web.application.contract.RepositoryFileUploadResult;
import io.jgitkins.web.application.contract.RepositoryOverviewResult;
import io.jgitkins.web.application.contract.RepositorySummary;
import io.jgitkins.web.application.port.out.RepositoryPort;
import io.jgitkins.web.infrastructure.cache.RepositoryFileIndexCacheSupport;
import io.jgitkins.web.infrastructure.cache.RepositoryTreeCacheSupport;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Primary
@RequiredArgsConstructor
public class CachedRepositoryPortAdapter implements RepositoryPort {

	private static final Duration TREE_CACHE_TTL = Duration.ofMinutes(5);
	private static final Duration FILE_INDEX_CACHE_TTL = Duration.ofMinutes(5);

	private final JGitkinsServerAdapter delegate;
	private final RepositoryTreeCacheSupport repositoryTreeCacheSupport;
	private final RepositoryFileIndexCacheSupport repositoryFileIndexCacheSupport;

	@Override
	public List<RepositorySummary> fetchRepositories() {
		return delegate.fetchRepositories();
	}

	@Override
	public List<RepositorySummary> fetchRepositoriesByUsername(String username) {
		return delegate.fetchRepositoriesByUsername(username);
	}

	@Override
	public RepositorySummary fetchRepository(Long repositoryId) {
		return delegate.fetchRepository(repositoryId);
	}

	@Override
	public RepositoryOverviewResult fetchRepositoryOverview(Long repositoryId, String branch) {
		return delegate.fetchRepositoryOverview(repositoryId, branch);
	}

	@Override
	public RepositoryOverviewResult fetchRepositoryOverviewByPath(String namespace, String repoName, String branch) {
		return delegate.fetchRepositoryOverviewByPath(namespace, repoName, branch);
	}

	@Override
	public List<CommitSummary> fetchCommits(String namespace, String repoName, String branch) {
		return delegate.fetchCommits(namespace, repoName, branch);
	}

	@Override
	public List<BranchSummary> fetchBranches(Long repositoryId) {
		return delegate.fetchBranches(repositoryId);
	}

	@Override
	public List<RepositoryFileEntry> fetchRepositoryFiles(String namespace, String repoName, String branch) {
		return delegate.fetchRepositoryFiles(namespace, repoName, branch);
	}

	@Override
	public List<RepositoryFileIndexEntry> fetchRepositoryFileIndex(String namespace, String repoName, String branch) {
		String selectedBranch = normalizeBranch(branch);
		String headCommit = resolveHeadCommit(namespace, repoName, selectedBranch);
		return repositoryFileIndexCacheSupport
				.get(namespace, repoName, selectedBranch, headCommit)
				.orElseGet(() -> {
					List<RepositoryFileIndexEntry> loaded = delegate.fetchRepositoryFileIndex(namespace, repoName, selectedBranch);
					repositoryFileIndexCacheSupport.put(namespace, repoName, selectedBranch, headCommit, loaded, FILE_INDEX_CACHE_TTL);
					return loaded;
				});
	}

	@Override
	public List<RepositoryFileEntry> fetchRepositoryTree(String namespace, String repoName, String branch, String directory) {
		String selectedBranch = normalizeBranch(branch);
		String normalizedDirectory = StringUtils.hasText(directory) ? directory.trim() : "";
		String headCommit = resolveHeadCommit(namespace, repoName, selectedBranch);
		return repositoryTreeCacheSupport
				.get(namespace, repoName, selectedBranch, normalizedDirectory, headCommit)
				.orElseGet(() -> {
					List<RepositoryFileEntry> loaded = delegate.fetchRepositoryTree(namespace, repoName, selectedBranch, normalizedDirectory);
					repositoryTreeCacheSupport.put(namespace, repoName, selectedBranch, normalizedDirectory, headCommit, loaded, TREE_CACHE_TTL);
					return loaded;
				});
	}

	@Override
	public RepositoryCreateResult createRepository(RepositoryCreateRequest request) {
		return delegate.createRepository(request);
	}

	@Override
	public RepositoryBranchCreateResult createBranch(Long repositoryId, String branchName, String sourceBranch) {
		return delegate.createBranch(repositoryId, branchName, sourceBranch);
	}

	@Override
	public RepositoryFileUploadResult uploadFile(RepositoryFileUploadRequest request) {
		return delegate.uploadFile(request);
	}

	private String normalizeBranch(String branch) {
		return StringUtils.hasText(branch) ? branch.trim() : "main";
	}

	private String resolveHeadCommit(String namespace, String repoName, String branch) {
		return delegate.fetchCommits(namespace, repoName, branch).stream()
				.findFirst()
				.map(CommitSummary::id)
				.orElse("no-head");
	}
}
