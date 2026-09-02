package io.jgitkins.server.change.review.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.change.review.application.contract.MergeRequest;
import io.jgitkins.server.change.review.application.contract.MergeResult;
import io.jgitkins.server.change.review.application.exception.BranchHeadNotFoundException;
import io.jgitkins.server.change.review.application.port.in.MergeUseCase;
import io.jgitkins.server.change.review.application.port.in.MergeabilityCheckUseCase;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
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



    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MergeController controller =
                new MergeController(mergeabilityCheckUseCase, mergeUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // standaloneSetup wires no Spring Security, so @AuthenticationPrincipal would resolve
                // to null without this and every request would look anonymous.
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation
                                .AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(ErrorStatusMappingTestConfig.realMapper())).build();
        signIn("7");
        this.objectMapper = new ObjectMapper();
    }

    private static void signIn(String subject) {
        io.jgitkins.server.support.TestAuthentication.authenticateAs(subject);
    }

    @AfterEach
    void signOut() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void checkMergeability_passesANullRequesterForAnAnonymousCaller() throws Exception {
        io.jgitkins.server.support.TestAuthentication.clear();
        // Null, not a rejection. A merge preview reads the repository, and a public repository is
        // readable while logged out. performMerge below is the one that demands a requester.
        when(mergeabilityCheckUseCase.checkMergeability("team", "repo", "feature", "main", null))
                .thenReturn(MergeResult.builder().status(MergeResult.Status.MERGEABLE)
                        .sourceBranch("feature").targetBranch("main").build());

        mockMvc.perform(get("/repositories/team/repo/merge/check")
                        .param("sourceBranch", "feature").param("targetBranch", "main"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(mergeabilityCheckUseCase)
                .checkMergeability("team", "repo", "feature", "main", null);
    }

    @Test
    void checkMergeability_returnsResult() throws Exception {
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGEABLE)
                .sourceBranch("feature")
                .targetBranch("main")
                .build();
        when(mergeabilityCheckUseCase.checkMergeability("team", "repo", "feature", "main", 7L))
                .thenReturn(result);

        mockMvc.perform(get("/repositories/team/repo/merge/check")
                        .param("sourceBranch", "feature")
                        .param("targetBranch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MERGEABLE"))
                .andExpect(jsonPath("$.data.sourceBranch").value("feature"));

        verify(mergeabilityCheckUseCase).checkMergeability("team", "repo", "feature", "main", 7L);
    }

    @Test
    void performMerge_returnsResult() throws Exception {
        MergeRequest request = new MergeRequest("feature", "main", "merge feature", "alice", "alice@test.com");
        MergeResult result = MergeResult.builder()
                .status(MergeResult.Status.MERGED)
                .newCommitId("abc123")
                .build();
        when(mergeUseCase.performMerge(eq("team"), eq("repo"), any(MergeRequest.class), eq(7L)))
                .thenReturn(result);

        mockMvc.perform(post("/repositories/team/repo/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MERGED"))
                .andExpect(jsonPath("$.data.newCommitId").value("abc123"));

        verify(mergeUseCase).performMerge(eq("team"), eq("repo"), any(MergeRequest.class), eq(7L));
    }

    @Test
    void performMerge_preservesBranchNotFoundWireContract() throws Exception {
        when(mergeUseCase.performMerge(eq("team"), eq("repo"), any(MergeRequest.class), eq(7L)))
                .thenThrow(new BranchHeadNotFoundException("missing"));

        mockMvc.perform(post("/repositories/team/repo/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new MergeRequest("missing", "main", null, "alice", "alice@test.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BRANCH-404"))
                .andExpect(jsonPath("$.error.message").value("Branch not found: missing"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }

    @Test
    void performMerge_rejectsAnAnonymousCallerWithoutReachingTheUseCase() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        mockMvc.perform(post("/repositories/team/repo/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new MergeRequest("feature", "main", null, "alice", "a@test.com"))))
                .andExpect(status().isUnauthorized());

        // Task 2.123. Anyone could merge any branch of any repository, private ones included.
        org.mockito.Mockito.verifyNoInteractions(mergeUseCase);
    }

    @Test
    void mergeRequest_carriesNoActorField() {
        // E3 guard. MergeRequest is bound from the HTTP body. An actor field on it would let the
        // caller name themselves, turning the authorization added in 2.123 into a formality.
        assertThat(java.util.Arrays.stream(MergeRequest.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("requesterUserId", "userId", "actorId", "requester");
    }
}