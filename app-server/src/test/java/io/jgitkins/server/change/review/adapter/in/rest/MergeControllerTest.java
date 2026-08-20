package io.jgitkins.server.change.review.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.change.review.application.dto.command.MergeRequest;
import io.jgitkins.server.change.review.application.dto.result.MergeResult;
import io.jgitkins.server.change.review.application.exception.BranchHeadNotFoundException;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MergeControllerTest {

    @Mock
    private MergeabilityCheckUseCase mergeabilityCheckUseCase;

    @Mock
    private MergeUseCase mergeUseCase;

    @Mock
    private CompositeErrorHttpStatusMapper statusMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MergeController controller = new MergeController(mergeabilityCheckUseCase, mergeUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(statusMapper)).build();
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

    @Test
    void performMerge_preservesBranchNotFoundWireContract() throws Exception {
        when(statusMapper.map(any())).thenReturn(HttpStatus.NOT_FOUND);
        when(mergeUseCase.performMerge(eq("team"), eq("repo"), any(MergeRequest.class)))
                .thenThrow(new BranchHeadNotFoundException("missing"));

        mockMvc.perform(post("/repositories/team/repo/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new MergeRequest("missing", "main", null, "alice", "alice@test.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BRANCH-404"))
                .andExpect(jsonPath("$.error.message").value("Branch not found: missing"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }
}
