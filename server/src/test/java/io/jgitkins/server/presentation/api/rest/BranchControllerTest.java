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
import io.jgitkins.server.application.dto.command.BranchCreateCommand;
import io.jgitkins.server.application.dto.result.BranchSearchResult;
import io.jgitkins.server.application.port.in.BranchCreateUseCase;
import io.jgitkins.server.application.port.in.BranchDeleteUseCase;
import io.jgitkins.server.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.presentation.dto.BranchCreateRequest;
import io.jgitkins.server.presentation.mapper.BranchRequestMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BranchController.class)
@AutoConfigureMockMvc(addFilters = false)
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BranchLoadUseCase branchLoadUseCase;

    @MockBean
    private BranchCreateUseCase branchCreateUseCase;

    @MockBean
    private BranchDeleteUseCase branchDeleteUseCase;

    @MockBean
    private BranchRequestMapper branchRequestMapper;

    @Test
    void create_returnsCreated() throws Exception {
        BranchCreateCommand command = BranchCreateCommand.builder()
                .repositoryId(1L)
                .branchName("feature")
                .sourceBranch("main")
                .build();
        when(branchRequestMapper.toCommand(any(Long.class), any(BranchCreateRequest.class))).thenReturn(command);

        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "branchName", "feature",
                                "sourceBranch", "main"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("feature")));

        verify(branchCreateUseCase).createBranch(command);
    }

    @Test
    void create_acceptsLegacyNameAlias() throws Exception {
        BranchCreateCommand command = BranchCreateCommand.builder()
                .repositoryId(1L)
                .branchName("feature-alias")
                .sourceBranch("main")
                .build();
        when(branchRequestMapper.toCommand(any(Long.class), any(BranchCreateRequest.class))).thenReturn(command);

        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "feature-alias",
                                "sourceBranch", "main"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("feature-alias")));

        verify(branchCreateUseCase).createBranch(command);
    }

    @Test
    void getBranches_returnsList() throws Exception {
        when(branchLoadUseCase.getBranches(1L)).thenReturn(List.of(
                BranchSearchResult.builder().repositoryId(1L).name("main").defaultBranch(true).build(),
                BranchSearchResult.builder().repositoryId(1L).name("feature").defaultBranch(false).build()
        ));

        mockMvc.perform(get("/api/repositories/1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("main"))
                .andExpect(jsonPath("$.data[0].defaultBranch").value(true))
                .andExpect(jsonPath("$.data[1].name").value("feature"));

        verify(branchLoadUseCase).getBranches(1L);
    }

    @Test
    void getBranch_returnsBranch() throws Exception {
        when(branchLoadUseCase.getBranch(1L, "feature"))
                .thenReturn(BranchSearchResult.builder().repositoryId(1L).name("feature").defaultBranch(false).build());

        mockMvc.perform(get("/api/repositories/1/branches/feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("feature"))
                .andExpect(jsonPath("$.data.defaultBranch").value(false));

        verify(branchLoadUseCase).getBranch(1L, "feature");
    }

    @Test
    void deleteBranch_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/1/branches/feature"))
                .andExpect(status().isNoContent());

        verify(branchDeleteUseCase).deleteBranch(1L, "feature");
    }
}
