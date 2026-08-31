package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.repository.application.contract.result.CommitHistory;
import io.jgitkins.server.repository.application.port.in.CommitLoadUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RepositoryCommitControllerTest {

    @Mock
    private CommitLoadUseCase commitLoadUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RepositoryCommitController controller = new RepositoryCommitController(
                commitLoadUseCase);
        // standaloneSetup builds no security filter chain, so @AuthenticationPrincipal has no
        // resolver unless one is registered here. Without it the parameter fails to resolve rather
        // than arriving null, and the failure reads as a routing problem.
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation
                                .AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void clearAuthentication() {
        io.jgitkins.server.support.TestAuthentication.clear();
    }

    @Test
    void getCommitDetail_returnsCommit() throws Exception {
        CommitHistory history = CommitHistory.builder()
                .id("c1")
                .authorName("alice")
                .shortMessage("init")
                .commitTime(LocalDateTime.now())
                .build();
        io.jgitkins.server.support.TestAuthentication.authenticateAs("7");
        when(commitLoadUseCase.getCommit("team", "repo", "c1", 7L)).thenReturn(history);

        mockMvc.perform(get("/repositories/team/repo/commits/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("c1"))
                .andExpect(jsonPath("$.data.authorName").value("alice"));

        verify(commitLoadUseCase).getCommit("team", "repo", "c1", 7L);
    }

    @Test
    void anonymousCallerReachesTheUseCaseWithANullRequester() throws Exception {
        // Null, not a rejection. A public repository's history stays readable while logged out, and
        // the visibility rule inside the use case is what decides. Answering 401 here would break
        // anonymous browsing of public repositories.
        when(commitLoadUseCase.getCommit("team", "repo", "c1", null))
                .thenReturn(CommitHistory.builder().id("c1").build());

        mockMvc.perform(get("/repositories/team/repo/commits/c1")).andExpect(status().isOk());

        verify(commitLoadUseCase).getCommit("team", "repo", "c1", null);
    }

    @Test
    void getBranchCommitHistories_returnsList() throws Exception {
        io.jgitkins.server.support.TestAuthentication.authenticateAs("7");
        when(commitLoadUseCase.getCommits("team", "repo", "main", 7L)).thenReturn(List.of(
                CommitHistory.builder().id("c1").shortMessage("a").build(),
                CommitHistory.builder().id("c2").shortMessage("b").build()
        ));

        mockMvc.perform(get("/repositories/team/repo/branches/main/commits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("c1"))
                .andExpect(jsonPath("$.data[1].id").value("c2"));

        verify(commitLoadUseCase).getCommits("team", "repo", "main", 7L);
    }
}
