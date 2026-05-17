package io.jgitkins.server.presentation.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.MergeRequest;
import io.jgitkins.server.application.dto.result.MergeResult;
import io.jgitkins.server.application.port.in.MergeUseCase;
import io.jgitkins.server.application.port.in.MergeabilityCheckUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MergeControllerTest {

    @Mock
    private MergeabilityCheckUseCase mergeabilityCheckUseCase;

    @Mock
    private MergeUseCase mergeUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MergeController controller = new MergeController(mergeabilityCheckUseCase, mergeUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void checkMergeability_returnsResult() throws Exception {
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGEABLE)
                .sourceBranch("feature")
                .targetBranch("main")
                .build();
        when(mergeabilityCheckUseCase.checkMergeability("team", "repo", "feature", "main"))
                .thenReturn(result);

        mockMvc.perform(get("/repositories/team/repo/merge/check")
                        .param("sourceBranch", "feature")
                        .param("targetBranch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MERGEABLE"))
                .andExpect(jsonPath("$.data.sourceBranch").value("feature"));

        verify(mergeabilityCheckUseCase).checkMergeability("team", "repo", "feature", "main");
    }

    @Test
    void performMerge_returnsResult() throws Exception {
        MergeRequest request = new MergeRequest("feature", "main", "merge feature", "alice", "alice@test.com");
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGED)
                .newCommitId("abc123")
                .build();
        when(mergeUseCase.performMerge(eq("team"), eq("repo"), any(MergeRequest.class))).thenReturn(result);

        mockMvc.perform(post("/repositories/team/repo/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MERGED"))
                .andExpect(jsonPath("$.data.newCommitId").value("abc123"));

        verify(mergeUseCase).performMerge(eq("team"), eq("repo"), any(MergeRequest.class));
    }
}
