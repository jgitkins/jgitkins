package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.CommitHistory;
import io.jgitkins.server.application.port.out.CommitGitPort;
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
}
