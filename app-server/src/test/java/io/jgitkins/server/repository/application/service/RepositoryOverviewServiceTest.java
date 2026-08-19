package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.result.FileEntry;
import io.jgitkins.server.repository.application.port.out.RepositoryActorPort;
import io.jgitkins.server.repository.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryPermission;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.support.GitRepositoryAccessService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryOverviewServiceTest {

	@Mock
	private RepositoryQueryPort repositoryQueryPort;

	@Mock
	private BranchQueryPort branchQueryPort;

	@Mock
	private FileGitPort fileGitPort;

	@Mock
	private RepositoryActorPort currentUserPort;

	@Mock
	private GitRepositoryAccessService gitRepositoryAccessService;

	@InjectMocks
	private RepositoryOverviewService service;

	@Test
	void getOverview_usesDefaultBranchAndLoadsTree() {
		RepositoryResult repository = new RepositoryResult(
				1L, "USER", "repo", "org/repo", "main", "PUBLIC",
				null, 1L, null, "org/repo.git", null, false,
				null, null, null);
		when(repositoryQueryPort.loadRepository(1L)).thenReturn(Optional.of(repository));

		List<BranchSearchResult> branches = List.of(
				new BranchSearchResult(1L, "main", false, false, true));
		when(branchQueryPort.findAllByRepositoryId(1L)).thenReturn(branches);

		List<FileEntry> tree = List.of(FileEntry.builder().name("README.md").build());
		when(fileGitPort.listTree("org", "repo", "main", "")).thenReturn(tree);
		when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.of(1L));
		when(gitRepositoryAccessService.resolvePermission(null, "org", "repo", 1L))
				.thenReturn(new RepositoryPermission("OWNER", true, true));

		RepositoryOverviewResult result = service.getOverview(1L, null);

		assertEquals("main", result.selectedBranch());
		assertEquals(tree, result.tree());
		assertEquals("OWNER", result.role());
		assertEquals(true, result.writable());
	}

	@Test
	void getOverviewByPath_loadsRepositoryByNamespaceAndName() {
		RepositoryResult repository = new RepositoryResult(
				1L, "USER", "repo", "org/repo", "main", "PUBLIC",
				null, 1L, null, "org/repo.git", null, false,
				null, null, null);
		when(repositoryQueryPort.loadRepositoryByPath("org", "repo")).thenReturn(Optional.of(repository));

		List<BranchSearchResult> branches = List.of(
				new BranchSearchResult(1L, "main", false, false, true));
		when(branchQueryPort.findAllByRepositoryId(1L)).thenReturn(branches);

		List<FileEntry> tree = List.of(FileEntry.builder().name("README.md").build());
		when(fileGitPort.listTree("org", "repo", "main", "")).thenReturn(tree);
		when(currentUserPort.resolveCurrentUserId()).thenReturn(Optional.empty());
		when(gitRepositoryAccessService.resolvePermission(null, "org", "repo", null))
				.thenReturn(new RepositoryPermission("PUBLIC_READ_ONLY", false, true));

		RepositoryOverviewResult result = service.getOverviewByPath("org", "repo", null);

		assertEquals("main", result.selectedBranch());
		assertEquals(tree, result.tree());
		assertEquals("PUBLIC_READ_ONLY", result.role());
		assertEquals(false, result.writable());
	}
}
