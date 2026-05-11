package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.CommitHistory;
import io.jgitkins.server.repository.application.exception.CommitNotFoundException;
import io.jgitkins.server.repository.application.port.out.CommitGitPort;
import io.jgitkins.server.repository.application.port.out.exception.GitCommitObjectMissingException;
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

    @InjectMocks
    private CommitService service;

    @Test
    void getCommit_delegatesToPort() throws IOException {
        CommitHistory history = new CommitHistory();
        when(commitGitPort.loadCommit("task", "repo", "hash")).thenReturn(history);

        CommitHistory result = service.getCommit("task", "repo", "hash");

        assertEquals(history, result);
    }

    @Test
    void getCommit_translatesGitCommitObjectMissingToApplicationException() {
        when(commitGitPort.loadCommit("task", "repo", "missing"))
                .thenThrow(new GitCommitObjectMissingException("missing"));

        assertThrows(CommitNotFoundException.class, () -> service.getCommit("task", "repo", "missing"));
    }
}
