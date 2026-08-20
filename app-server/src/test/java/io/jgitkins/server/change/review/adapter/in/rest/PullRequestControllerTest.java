package io.jgitkins.server.change.review.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.change.review.application.dto.command.PullRequestCreateCommand;
import io.jgitkins.server.change.review.application.dto.result.PullRequestDetailResult;
import io.jgitkins.server.change.review.application.dto.result.PullRequestResult;
import io.jgitkins.server.change.review.application.exception.RepositoryReferenceNotFoundException;
import io.jgitkins.server.change.review.application.port.in.CreatePullRequestUseCase;
import io.jgitkins.server.change.review.application.port.in.GetPullRequestDetailUseCase;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
import io.jgitkins.server.change.review.adapter.in.rest.dto.request.PullRequestCreateRequest;
import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import io.jgitkins.server.change.review.domain.model.TargetDrift;
import io.jgitkins.server.change.review.domain.model.vo.PullRequestId;
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
class PullRequestControllerTest {

    @Mock
    private CreatePullRequestUseCase createPullRequestUseCase;

    @Mock
    private GetPullRequestDetailUseCase getPullRequestDetailUseCase;

    @Mock
    private CompositeErrorHttpStatusMapper statusMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PullRequestController controller = new PullRequestController(createPullRequestUseCase, getPullRequestDetailUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(statusMapper)).build();
        this.objectMapper = new ObjectMapper();
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
        when(createPullRequestUseCase.createPullRequest(any(PullRequestCreateCommand.class))).thenReturn(result);

        mockMvc.perform(post("/repositories/team/repo/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new PullRequestCreateRequest("feature", "main"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(createPullRequestUseCase).createPullRequest(any(PullRequestCreateCommand.class));
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
        when(getPullRequestDetailUseCase.getPullRequestDetail(PullRequestId.of(10L))).thenReturn(result);

        mockMvc.perform(get("/repositories/team/repo/pull-requests/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(getPullRequestDetailUseCase).getPullRequestDetail(PullRequestId.of(10L));
    }

    @Test
    void createPullRequest_preservesRepositoryNotFoundWireContract() throws Exception {
        when(statusMapper.map(any())).thenReturn(HttpStatus.NOT_FOUND);
        when(createPullRequestUseCase.createPullRequest(any(PullRequestCreateCommand.class)))
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
