package io.jgitkins.server.repository.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.RepositoryResult;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.port.out.BranchQueryPort;
import io.jgitkins.server.repository.application.port.out.FileGitPort;
import io.jgitkins.server.repository.application.port.out.RepositoryQueryPort;
import io.jgitkins.server.repository.application.service.internal.GitRepositoryAccessService;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The overview route's visibility gate.
 *
 * <p>Why this test exists rather than trusting the route's shape: {@code buildOverview} already took
 * a requester and already resolved a permission before task P0a, and used it only to populate
 * {@code role} and {@code writable} in the response. A reader of that method would reasonably
 * conclude the route was authorized. It was not — the branch list and root file tree of a private
 * repository came back to anyone.
 *
 * <p>Both assertions are about ordering. The gate has to run before {@code branchQueryPort} and
 * {@code fileGitPort}, because a gate that runs after them has already read the data it was meant to
 * withhold, and answering 404 at that point protects nothing but the response body.
 */
class RepositoryOverviewServiceTest {

    private final RepositoryQueryPort repositoryQueryPort = mock(RepositoryQueryPort.class);
    private final BranchQueryPort branchQueryPort = mock(BranchQueryPort.class);
    private final FileGitPort fileGitPort = mock(FileGitPort.class);
    private final GitRepositoryAccessService accessService = mock(GitRepositoryAccessService.class);
    private final RepositoryAccessValidator validator = mock(RepositoryAccessValidator.class);

    private final RepositoryOverviewService service = new RepositoryOverviewService(
            repositoryQueryPort, branchQueryPort, fileGitPort, accessService, validator);

    private final RepositoryResult repository = mock(RepositoryResult.class);

    @Test
    void getOverviewRefusesBeforeReadingBranchesOrTheTree() {
        when(repositoryQueryPort.loadRepository(1L)).thenReturn(Optional.of(repository));
        doThrow(new RepositoryNotFoundException())
                .when(validator).validateReadAccess(any(RepositoryResult.class), any());

        assertThatThrownBy(() -> service.getOverview(null, 1L, "main"))
                .isInstanceOf(RepositoryNotFoundException.class);

        verifyNoInteractions(branchQueryPort, fileGitPort);
    }

    @Test
    void getOverviewByPathRefusesBeforeReadingBranchesOrTheTree() {
        when(repositoryQueryPort.loadRepositoryByPath("alice", "demo"))
                .thenReturn(Optional.of(repository));
        doThrow(new RepositoryNotFoundException())
                .when(validator).validateReadAccess(any(RepositoryResult.class), any());

        assertThatThrownBy(() -> service.getOverviewByPath(null, "alice", "demo", "main"))
                .isInstanceOf(RepositoryNotFoundException.class);

        verifyNoInteractions(branchQueryPort, fileGitPort);
    }
}
