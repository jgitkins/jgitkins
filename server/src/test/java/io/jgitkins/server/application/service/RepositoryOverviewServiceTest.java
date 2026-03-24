package io.jgitkins.server.application.service;

import io.jgitkins.server.application.dto.FileEntry;
import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.dto.result.RepositoryOverviewResult;
import io.jgitkins.server.application.dto.result.RepositoryResult;
import io.jgitkins.server.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.application.port.in.FileTreeLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.out.CurrentUserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryOverviewServiceTest {

    @Mock
    private RepositoryLoadUseCase repositoryLoadUseCase;

    @Mock
    private BranchLoadUseCase branchLoadUseCase;

    @Mock
    private FileTreeLoadUseCase fileTreeLoadUseCase;

    @Mock
    private CurrentUserPort currentUserPersistencePort;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @InjectMocks
    private RepositoryOverviewService service;

    @Test
    void getOverview_usesDefaultBranchAndLoadsTree() {
        RepositoryResult repository = RepositoryResult.builder()
                .clonePath("org/repo.git")
                .path("org/repo")
                .build();
        when(repositoryLoadUseCase.getRepository(1L)).thenReturn(repository);

        List<BranchSearchResult> branches = List.of(
                BranchSearchResult.builder().name("main").defaultBranch(true).build());
        when(branchLoadUseCase.getBranches(1L)).thenReturn(branches);

        List<FileEntry> tree = List.of(FileEntry.builder().name("README.md").build());
        when(fileTreeLoadUseCase.getTree("org", "repo", "main", "")).thenReturn(tree);
        when(currentUserPersistencePort.resolveCurrentUserId()).thenReturn(Optional.of(1L));
        when(gitRepositoryAccessService.resolvePermission(null, "org", "repo", 1L))
                .thenReturn(new GitRepositoryAccessService.RepositoryPermission("OWNER", true, true));

        RepositoryOverviewResult result = service.getOverview(1L, null);

        assertEquals("main", result.getSelectedBranch());
        assertEquals(tree, result.getTree());
        assertEquals("OWNER", result.getRole());
        assertEquals(true, result.isWritable());
    }
}
