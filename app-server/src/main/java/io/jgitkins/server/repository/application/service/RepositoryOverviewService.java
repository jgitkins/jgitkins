package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.contract.internal.RepositoryKey;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

	private static final String ROOT_PATH = "";

	private final RepositoryQueryPort repositoryQueryPort;
	private final BranchQueryPort branchQueryPort;
	private final FileGitPort fileGitPort;
	private final RepositoryActorPort currentUserPort;
	private final GitRepositoryAccessService gitRepositoryAccessService;

	@Override
	public RepositoryOverviewResult getOverview(Long repositoryId, String branch) {
		RepositoryResult repository = repositoryQueryPort.loadRepository(repositoryId)
				.orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
		return buildOverview(repository, branch);
	}

	@Override
	public RepositoryOverviewResult getOverviewByPath(String namespace, String repoName, String branch) {
		RepositoryResult repository = repositoryQueryPort.loadRepositoryByPath(namespace, repoName)
				.orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
		return buildOverview(repository, branch);
	}

	private RepositoryOverviewResult buildOverview(RepositoryResult repository, String branch) {
		RepositoryKey key = resolveRepositoryKey(repository);
		List<BranchSearchResult> branches = branchQueryPort.findAllByRepositoryId(repository.id());
		String selectedBranch = resolveBranch(branch, branches);
		List<FileEntry> tree = fileGitPort.listTree(key.namespace(), key.repoName(), selectedBranch, ROOT_PATH);
		Long userId = currentUserPort.resolveCurrentUserId().orElse(null);
		RepositoryPermission permission = gitRepositoryAccessService.resolvePermission(
				null,
				key.namespace(),
				key.repoName(),
				userId);

		return new RepositoryOverviewResult(
				repository,
				branches,
				tree,
				selectedBranch,
				permission.role(),
				permission.writable()
		);
	}

	private String resolveBranch(String branch, List<BranchSearchResult> branches) {
		if (StringUtils.hasText(branch)) {
			return branch;
		}

		return branches.stream()
				.filter(BranchSearchResult::defaultBranch)
				.findFirst()
				.map(BranchSearchResult::name)
				.orElseGet(() -> branches.isEmpty() ? null : branches.get(0).name());
	}

	private RepositoryKey resolveRepositoryKey(RepositoryResult repository) {
		RepositoryKey key = RepositoryKey.fromPath(repository.clonePath());
		return key != null ? key : RepositoryKey.fromPath(repository.path());
	}
}
