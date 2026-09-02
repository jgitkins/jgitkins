package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.contract.result.RepositoryKey;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import io.jgitkins.core.common.exception.JgitkinsException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchLoadServiceTest {

    @Mock
    private BranchQueryPort branchQueryPort;

    @Mock
    private RepositoryLoadUseCase repositoryLoadUseCase;

    @Mock
    private RepositoryAccessValidator repositoryAccessValidator;

    private void repositoryResolvesTo(String namespace, String repoName) {
        when(repositoryLoadUseCase.resolveRepositoryKey(1L))
                .thenReturn(Optional.of(new RepositoryKey(namespace, repoName)));
    }

    @InjectMocks
    private BranchLoadService service;

    @Test
    void loadBranch_throwsWhenBranchMissing() {
        repositoryResolvesTo("task", "repo");
        when(branchQueryPort.findByRepositoryIdAndName(1L, "missing")).thenReturn(Optional.empty());

        assertThrows(JgitkinsException.class, () -> service.loadBranch(1L, "missing", 7L));
    }

    @Test
    void loadBranches_refusesBeforeQueryingWhenTheRepositoryIsNotVisible() {
        // Not-found for a repository the caller cannot see, and the branch query is never reached --
        // otherwise the branch names would have been loaded before the denial.
        repositoryResolvesTo("task", "repo");
        org.mockito.Mockito.doThrow(new RepositoryNotFoundException())
                .when(repositoryAccessValidator).validateReadAccess("task", "repo", null);

        assertThrows(RepositoryNotFoundException.class, () -> service.loadBranches(1L, null));

        org.mockito.Mockito.verifyNoInteractions(branchQueryPort);
    }

    @Test
    void loadBranches_answersNotFoundForAnIdThatDoesNotResolve() {
        // Same exception as "exists but not visible", deliberately: a different answer here would
        // tell an anonymous caller which repository ids exist.
        when(repositoryLoadUseCase.resolveRepositoryKey(404L)).thenReturn(Optional.empty());

        assertThrows(RepositoryNotFoundException.class, () -> service.loadBranches(404L, null));
    }

    @Test
    void loadBranches_returnsQueryResults() {
        repositoryResolvesTo("task", "repo");
        when(branchQueryPort.findAllByRepositoryId(1L)).thenReturn(
                java.util.List.of(new BranchSearchResult(1L, "main", false, false, true))
        );

        java.util.List<BranchSearchResult> results = service.loadBranches(1L, 7L);

        org.junit.jupiter.api.Assertions.assertEquals(1, results.size());
        org.junit.jupiter.api.Assertions.assertEquals("main", results.get(0).name());
    }
}
