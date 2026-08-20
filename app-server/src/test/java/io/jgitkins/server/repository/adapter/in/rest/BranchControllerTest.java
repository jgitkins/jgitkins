package io.jgitkins.server.repository.adapter.in.rest;

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
import io.jgitkins.server.repository.application.contract.command.BranchCreateCommand;
import io.jgitkins.server.repository.application.contract.result.BranchSearchResult;
import io.jgitkins.server.repository.application.port.in.BranchLoadUseCase;
import io.jgitkins.server.repository.application.port.in.BranchManagementUseCase;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.BranchCreateRequest;
import io.jgitkins.server.repository.adapter.in.rest.mapper.BranchRequestMapper;
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
    private BranchManagementUseCase branchManagementUseCase;

    @MockBean
    private BranchRequestMapper branchRequestMapper;

    @Test
    void create_returnsCreated() throws Exception {
        BranchCreateCommand command = new BranchCreateCommand(1L, "feature", "main", false);
        when(branchRequestMapper.toCommand(any(Long.class), any(BranchCreateRequest.class))).thenReturn(command);

        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "branchName", "feature",
                                "sourceBranch", "main"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("feature")))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(branchManagementUseCase).createBranch(command);
    }

    @Test
    void create_acceptsLegacyNameAlias() throws Exception {
        BranchCreateCommand command = new BranchCreateCommand(1L, "feature-alias", "main", false);
        when(branchRequestMapper.toCommand(any(Long.class), any(BranchCreateRequest.class))).thenReturn(command);

        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "feature-alias",
                                "sourceBranch", "main"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("feature-alias")))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(branchManagementUseCase).createBranch(command);
    }

    @Test
    void getBranches_returnsList() throws Exception {
        when(branchLoadUseCase.loadBranches(1L)).thenReturn(List.of(
                new BranchSearchResult(1L, "main", false, false, true),
                new BranchSearchResult(1L, "feature", false, false, false)
        ));

        mockMvc.perform(get("/api/repositories/1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("main"))
                .andExpect(jsonPath("$.data[0].defaultBranch").value(true))
                .andExpect(jsonPath("$.data[1].name").value("feature"));

        verify(branchLoadUseCase).loadBranches(1L);
    }

    @Test
    void getBranch_returnsBranch() throws Exception {
        when(branchLoadUseCase.loadBranch(1L, "feature"))
                .thenReturn(new BranchSearchResult(1L, "feature", false, false, false));

        mockMvc.perform(get("/api/repositories/1/branches/feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("feature"))
                .andExpect(jsonPath("$.data.defaultBranch").value(false));

        verify(branchLoadUseCase).loadBranch(1L, "feature");
    }

    @Test
    void deleteBranch_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/1/branches/feature"))
                .andExpect(status().isNoContent());

        verify(branchManagementUseCase).deleteBranch(1L, "feature");
    }
}
