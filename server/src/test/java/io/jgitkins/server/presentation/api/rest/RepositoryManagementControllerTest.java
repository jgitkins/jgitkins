package io.jgitkins.server.presentation.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryCreateUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryDeleteUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.domain.model.vo.OwnerType;
import io.jgitkins.server.presentation.dto.RepositoryCreateRequest;
import io.jgitkins.server.presentation.mapper.RepositoryRequestMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class RepositoryManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepositoryCreateUseCase repositoryCreateUseCase;

    @MockBean
    private RepositoryLoadUseCase repositoryLoadUseCase;

    @MockBean
    private RepositoryDeleteUseCase repositoryDeleteUseCase;

    @MockBean
    private RepositoryOverviewUseCase repositoryOverviewUseCase;

    @MockBean
    private RepositoryRequestMapper repositoryRequestMapper;

    @Test
    void create_returnsCreatedResponse() throws Exception {
        RepositoryCreateCommand command = RepositoryCreateCommand.builder()
                .repoName("sample-repo")
                .ownerType(OwnerType.USER)
                .mainBranch("main")
                .build();
        RepositoryResult result = new RepositoryResult(100L, "USER", "sample-repo", null, null, null, null, null, null, null, null, false, null, null, null);

        when(repositoryRequestMapper.toCommand(any(RepositoryCreateRequest.class))).thenReturn(command);
        when(repositoryCreateUseCase.create(command)).thenReturn(result);

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "repoName", "sample-repo",
                                "mainBranch", "main",
                                "ownerType", "USER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/repositories/100")))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.name").value("sample-repo"));

        verify(repositoryCreateUseCase).create(command);
    }

    @Test
    void getRepository_returnsMetadata() throws Exception {
        when(repositoryLoadUseCase.loadRepository(1L))
                .thenReturn(new RepositoryResult(1L, null, "repo-1", null, null, null, null, null, null, null, null, false, null, null, null));

        mockMvc.perform(get("/api/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("repo-1"));

        verify(repositoryLoadUseCase).loadRepository(1L);
    }

    @Test
    void getRepositories_returnsList() throws Exception {
        when(repositoryLoadUseCase.loadRepositories()).thenReturn(List.of(
                new RepositoryResult(1L, null, "repo-1", null, null, null, null, null, null, null, null, false, null, null, null),
                new RepositoryResult(2L, null, "repo-2", null, null, null, null, null, null, null, null, false, null, null, null)
        ));

        mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("repo-1"))
                .andExpect(jsonPath("$.data[1].name").value("repo-2"));

        verify(repositoryLoadUseCase).loadRepositories();
    }

    @Test
    void getUserRepositories_returnsList() throws Exception {
        when(repositoryLoadUseCase.loadUserRepositories("alice"))
                .thenReturn(List.of(new RepositoryResult(3L, null, "alice-repo", null, null, null, null, null, null, null, null, false, null, null, null)));

        mockMvc.perform(get("/api/repositories/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3L))
                .andExpect(jsonPath("$.data[0].name").value("alice-repo"));

        verify(repositoryLoadUseCase).loadUserRepositories("alice");
    }

    @Test
    void deleteRepository_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/9"))
                .andExpect(status().isNoContent());

        verify(repositoryDeleteUseCase).deleteRepository(9L);
    }
}
