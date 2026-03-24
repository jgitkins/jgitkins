package io.jgitkins.server.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.RepositoryKey;
import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.application.port.in.GitRepositoryAccessUseCase;
import io.jgitkins.server.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepositoryOverviewService implements RepositoryOverviewUseCase {

	private static final String ROOT_PATH = "";

	private final RepositoryLoadUseCase repositoryLoadUseCase;
	private final BranchLoadUseCase branchLoadUseCase;
	private final FileTreeLoadUseCase fileTreeLoadUseCase;
	private final CurrentUserPort currentUserPersistencePort;
	private final GitRepositoryAccessUseCase gitRepositoryAccessUseCase;

	@Override
	public RepositoryOverviewResult getOverview(Long repositoryId, String branch) {

		RepositoryResult repository = repositoryLoadUseCase.getRepository(repositoryId);

		RepositoryKey key = resolveRepositoryKey(repository);

		List<BranchSearchResult> branches = branchLoadUseCase.getBranches(repositoryId);

		String selectedBranch = resolveBranch(branch, branches);

		List<FileEntry> tree = fileTreeLoadUseCase.getTree(key.namespace(), key.repoName(), selectedBranch, ROOT_PATH);

		Long userId = currentUserPersistencePort.resolveCurrentUserId().orElse(null);

		GitRepositoryAccessUseCase.RepositoryPermission permission = gitRepositoryAccessUseCase.resolvePermission(null,
				key != null ? key.namespace() : null, key != null ? key.repoName() : null, userId);

		return RepositoryOverviewResult.builder()
				.repository(repository)
				.branches(branches)
				.tree(tree)
				.selectedBranch(selectedBranch)
				.role(permission.role())
				.writable(permission.writable())
				.build();
	}

	private String resolveBranch(String branch, List<BranchSearchResult> branches) {
		if (StringUtils.hasText(branch)) {
			return branch;
		}

		return branches.stream()
				.filter(b -> b.isDefaultBranch())
				.findFirst()
				.map(BranchSearchResult::getName)
				.orElseGet(() -> branches.isEmpty() ? null : branches.get(0).getName());
	}

	private RepositoryKey resolveRepositoryKey(RepositoryResult repository) {
		// TODO: repository 필수 값 여부는 상위 계층 호출 전 혹은 Controller 검증 단에서 처리
		RepositoryKey key = RepositoryKey.fromPath(repository.getClonePath());
		return key != null ? key : RepositoryKey.fromPath(repository.getPath());
	}
}
