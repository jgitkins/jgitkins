package io.jgitkins.server.change.review.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.change.review.application.contract.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.contract.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.contract.result.PullRequestResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.change.review.application.port.in.GetPullRequestDetailUseCase;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.change.review.adapter.in.rest.contract.request.PullRequestCreateRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import java.time.LocalDateTime;
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
class PullRequestManagementControllerTest {

    @Mock
    private CreatePullRequestUseCase createPullRequestUseCase;

    @Mock
    private GetPullRequestDetailUseCase getPullRequestDetailUseCase;


    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PullRequestManagementController controller = new PullRequestManagementController(createPullRequestUseCase,
                getPullRequestDetailUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // standaloneSetup wires no Spring Security, so @AuthenticationPrincipal has no
                // resolver without this and the parameter fails to resolve rather than arriving null.
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation
                                .AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(ErrorStatusMappingTestConfig.realMapper())).build();
        this.objectMapper = new ObjectMapper();
        // Opening a pull request is a write and now requires a principal. MockMvc.principal() would
        // not reach @AuthenticationPrincipal, which resolves from SecurityContextHolder.
        io.jgitkins.server.support.TestAuthentication.authenticateAs("7");
    }

    @org.junit.jupiter.api.Test
    void getPullRequestDetail_passesANullRequesterForAnAnonymousCaller() throws Exception {
        // Reading a pull request on a public repository must keep working while logged out, so the
        // route passes null rather than refusing. Creating one, above, does require a principal --
        // that asymmetry is the read/write split, and this test is what pins it.
        io.jgitkins.server.support.TestAuthentication.clear();
        when(getPullRequestDetailUseCase.getPullRequestDetail(PullRequestId.of(10L), null))
                .thenReturn(null);

        mockMvc.perform(get("/repositories/team/repo/pull-requests/10"));

        verify(getPullRequestDetailUseCase).getPullRequestDetail(PullRequestId.of(10L), null);
    }

    @org.junit.jupiter.api.Test
    void createPullRequest_refusesAnAnonymousCallerBeforeTheUseCase() throws Exception {
        // Opening a pull request writes. Until task P0a this route took no principal at all, so an
        // anonymous caller could open one on any repository including a private one.
        io.jgitkins.server.support.TestAuthentication.clear();

        mockMvc.perform(post("/repositories/team/repo/pull-requests")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"sourceBranch\":\"feature\",\"targetBranch\":\"main\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createPullRequestUseCase);
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() {
        io.jgitkins.server.support.TestAuthentication.clear();
    }

    @Test
    void createPullRequest_returnsCreatedApiResponse() throws Exception {
        PullRequestResult result = PullRequestResult.builder()
                .id(10L)
                .repositoryId(1L)
                .source(BranchHeadSnapshot.of("feature", "aaaaaaa"))
                .target(BranchHeadSnapshot.of("main", "bbbbbbb"))
                .status(PullRequestStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(createPullRequestUseCase.createPullRequest(any(PullRequestCreateCommand.class), eq(7L))).thenReturn(result);

        mockMvc.perform(post("/repositories/team/repo/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new PullRequestCreateRequest("feature", "main"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(createPullRequestUseCase).createPullRequest(any(PullRequestCreateCommand.class), eq(7L));
    }

    @Test
    void getPullRequestDetail_returnsDetailApiResponse() throws Exception {
        PullRequestDetailResult result = PullRequestDetailResult.builder()
                .id(10L)
                .repositoryId(1L)
                .storedSource(BranchHeadSnapshot.of("feature", "aaaaaaa"))
                .storedTarget(BranchHeadSnapshot.of("main", "bbbbbbb"))
                .currentSource(BranchHeadSnapshot.of("feature", "aaaaaaa"))
                .currentTarget(BranchHeadSnapshot.of("main", "bbbbbbb"))
                .status(PullRequestStatus.OPEN)
                .targetDrift(TargetDrift.none())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(getPullRequestDetailUseCase.getPullRequestDetail(PullRequestId.of(10L), 7L)).thenReturn(result);

        mockMvc.perform(get("/repositories/team/repo/pull-requests/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(getPullRequestDetailUseCase).getPullRequestDetail(PullRequestId.of(10L), 7L);
    }

    @Test
    void createPullRequest_preservesRepositoryNotFoundWireContract() throws Exception {
        when(createPullRequestUseCase.createPullRequest(any(PullRequestCreateCommand.class), eq(7L)))
                .thenThrow(new RepositoryReferenceNotFoundException("team", "repo"));

        mockMvc.perform(post("/repositories/team/repo/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new PullRequestCreateRequest("feature", "main"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REPO-404"))
                .andExpect(jsonPath("$.error.message").value("Repository not found: team/repo"))
                .andExpect(jsonPath("$.error.source").value("application"));
    }
}
