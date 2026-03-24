package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.application.port.out.MergeGitPort;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MergeServiceTest {

    @Mock
    private MergeGitPort mergeGitPort;

    @InjectMocks
    private MergeService service;

    @Test
    void checkMergeability_delegatesToPort() throws IOException {
        MergeResult result = MergeResult.builder().build();
        when(mergeGitPort.previewMerge("task", "repo", "src", "dst")).thenReturn(result);

        MergeResult response = service.checkMergeability("task", "repo", "src", "dst");

        assertEquals(result, response);
    }

    @Test
    void performMerge_delegatesToPort() throws IOException {
        MergeResult result = MergeResult.builder().build();
        MergeRequest request = new MergeRequest();
        when(mergeGitPort.merge("task", "repo", request)).thenReturn(result);

        MergeResult response = service.performMerge("task", "repo", request);

        assertEquals(result, response);
    }
}
