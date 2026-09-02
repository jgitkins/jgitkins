package io.jgitkins.server.repository.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.repository.application.contract.CommitHistory;
import io.jgitkins.server.repository.application.exception.CommitNotFoundException;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
import io.jgitkins.server.repository.application.validate.RepositoryAccessValidator;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommitServiceTest {

    @Mock
    private CommitGitPort commitGitPort;

    @Mock
    private RepositoryAccessValidator repositoryAccessValidator;

    @InjectMocks
    private CommitService service;

    @Test
    void getCommit_delegatesToPort() throws IOException {
        CommitHistory history = new CommitHistory();
        when(commitGitPort.loadCommit("task", "repo", "hash")).thenReturn(history);

        CommitHistory result = service.getCommit("task", "repo", "hash", 7L);

        assertEquals(history, result);
    }

    @Test
    void getCommit_refusesBeforeReadingGitWhenTheRepositoryIsNotVisible() {
        // The guard runs before the port, so a caller who cannot see the repository never reaches
        // disk. Asserting the port was not touched is the part that matters: a guard placed after
        // the read would still answer 404 while having already loaded the commit.
        org.mockito.Mockito.doThrow(new io.jgitkins.server.repository.application.exception
                        .RepositoryNotFoundException())
                .when(repositoryAccessValidator).validateReadAccess("task", "repo", null);

        assertThrows(io.jgitkins.server.repository.application.exception.RepositoryNotFoundException.class,
                () -> service.getCommit("task", "repo", "hash", null));

        org.mockito.Mockito.verifyNoInteractions(commitGitPort);
    }

    @Test
    void getCommits_refusesBeforeReadingGitWhenTheRepositoryIsNotVisible() {
        org.mockito.Mockito.doThrow(new io.jgitkins.server.repository.application.exception
                        .RepositoryNotFoundException())
                .when(repositoryAccessValidator).validateReadAccess("task", "repo", null);

        assertThrows(io.jgitkins.server.repository.application.exception.RepositoryNotFoundException.class,
                () -> service.getCommits("task", "repo", "main", null));

        org.mockito.Mockito.verifyNoInteractions(commitGitPort);
    }

    @Test
    void getCommit_translatesGitCommitObjectMissingToApplicationException() {
        when(commitGitPort.loadCommit("task", "repo", "missing"))
                .thenThrow(new GitCommitObjectMissingException("missing"));

        assertThrows(CommitNotFoundException.class, () -> service.getCommit("task", "repo", "missing", 7L));
    }
}
