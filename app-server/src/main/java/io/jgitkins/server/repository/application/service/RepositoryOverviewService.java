package io.jgitkins.server.repository.application.service;

import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.contract.internal.RepositoryKey;
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
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
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
	private final GitRepositoryAccessService gitRepositoryAccessService;
	private final RepositoryAccessValidator repositoryAccessValidator;

	@Override
	public RepositoryOverviewResult getOverview(Long requesterUserId, Long repositoryId, String branch) {
		RepositoryResult repository = repositoryQueryPort.loadRepository(repositoryId)
				.orElseThrow(() -> new RepositoryNotFoundException(repositoryId));
		return buildOverview(repository, branch, requesterUserId);
	}

	@Override
	public RepositoryOverviewResult getOverviewByPath(Long requesterUserId, String namespace,
			String repoName, String branch) {
		RepositoryResult repository = repositoryQueryPort.loadRepositoryByPath(namespace, repoName)
				.orElseThrow(() -> new RepositoryNotFoundException(namespace, repoName));
		return buildOverview(repository, branch, requesterUserId);
	}

	private RepositoryOverviewResult buildOverview(RepositoryResult repository, String branch,
			Long requesterUserId) {
		// First, before any branch or tree is read.
		//
		// This method already took the requester and already resolved a permission -- but only to
		// fill role and writable into the response. Nothing gated on it, so the branch list and the
		// root file tree of a private repository came back to an anonymous caller. Having the
		// permission in hand and not acting on it is worse than not having it: the route looks
		// authorized to anyone reading it.
		//
		// The gate goes through the validator rather than reading visibleOn here, so the rule lives
		// in one place. The validator resolves the permission again from the same loaded result,
		// which is a second in-memory call on data already fetched.
		repositoryAccessValidator.validateReadAccess(repository, requesterUserId);
		RepositoryKey key = resolveRepositoryKey(repository);
		List<BranchSearchResult> branches = branchQueryPort.findAllByRepositoryId(repository.id());
		String selectedBranch = resolveBranch(branch, branches);
		List<FileEntry> tree = fileGitPort.listTree(key.namespace(), key.repoName(), selectedBranch, ROOT_PATH);
		RepositoryPermission permission = gitRepositoryAccessService.resolvePermission(
				null,
				key.namespace(),
				key.repoName(),
				requesterUserId);

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
