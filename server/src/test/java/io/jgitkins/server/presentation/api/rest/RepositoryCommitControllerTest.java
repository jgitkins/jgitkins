package io.jgitkins.server.presentation.api.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.application.dto.CommitHistory;
import io.jgitkins.server.application.port.in.CommitLoadUseCase;
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
        RepositoryCommitController controller = new RepositoryCommitController(commitLoadUseCase);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getCommitDetail_returnsCommit() throws Exception {
        CommitHistory history = CommitHistory.builder()
                .id("c1")
                .authorName("alice")
                .shortMessage("init")
                .commitTime(LocalDateTime.now())
                .build();
        when(commitLoadUseCase.getCommit("team", "repo", "c1")).thenReturn(history);

        mockMvc.perform(get("/repositories/team/repo/commits/c1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("c1"))
                .andExpect(jsonPath("$.data.authorName").value("alice"));

        verify(commitLoadUseCase).getCommit("team", "repo", "c1");
    }

    @Test
    void getBranchCommitHistories_returnsList() throws Exception {
        when(commitLoadUseCase.getCommits("team", "repo", "main")).thenReturn(List.of(
                CommitHistory.builder().id("c1").shortMessage("a").build(),
                CommitHistory.builder().id("c2").shortMessage("b").build()
        ));

        mockMvc.perform(get("/repositories/team/repo/branches/main/commits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("c1"))
                .andExpect(jsonPath("$.data[1].id").value("c2"));

        verify(commitLoadUseCase).getCommits("team", "repo", "main");
    }
}
