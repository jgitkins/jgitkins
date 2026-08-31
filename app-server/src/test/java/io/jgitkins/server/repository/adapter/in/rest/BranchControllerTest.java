package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import io.jgitkins.server.support.TestAuthentication;
import org.springframework.context.annotation.Import;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
@Import({ErrorStatusMappingTestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class BranchControllerTest {

    @BeforeEach
    void authenticateRequester() {
        TestAuthentication.authenticateAs("7");
    }

    @AfterEach
    void clearAuthentication() {
        TestAuthentication.clear();
    }

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
        BranchCreateCommand command = new BranchCreateCommand(7L, 1L, "feature", "main", false);
        when(branchRequestMapper.toCommand(anyLong(), anyLong(), any(BranchCreateRequest.class))).thenReturn(command);

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
        BranchCreateCommand command = new BranchCreateCommand(7L, 1L, "feature-alias", "main", false);
        when(branchRequestMapper.toCommand(anyLong(), anyLong(), any(BranchCreateRequest.class))).thenReturn(command);

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
    void getBranches_passesTheRequesterSoTheVisibilityRuleCanRun() throws Exception {
        // The point of the parameter: before task P0a the route sent only the id, so the branch list
        // of any private repository came back to anyone. Asserting the requester reaches the use case
        // is what makes the rule reachable at all.
        when(branchLoadUseCase.loadBranches(9L, 7L)).thenReturn(List.of());

        mockMvc.perform(get("/api/repositories/9/branches")).andExpect(status().isOk());

        verify(branchLoadUseCase).loadBranches(9L, 7L);
    }

    @Test
    void getBranches_returnsList() throws Exception {
        when(branchLoadUseCase.loadBranches(1L, 7L)).thenReturn(List.of(
                new BranchSearchResult(1L, "main", false, false, true),
                new BranchSearchResult(1L, "feature", false, false, false)
        ));

        mockMvc.perform(get("/api/repositories/1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("main"))
                .andExpect(jsonPath("$.data[0].defaultBranch").value(true))
                .andExpect(jsonPath("$.data[1].name").value("feature"));

        verify(branchLoadUseCase).loadBranches(1L, 7L);
    }

    @Test
    void getBranch_returnsBranch() throws Exception {
        when(branchLoadUseCase.loadBranch(1L, "feature", 7L))
                .thenReturn(new BranchSearchResult(1L, "feature", false, false, false));

        mockMvc.perform(get("/api/repositories/1/branches/feature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("feature"))
                .andExpect(jsonPath("$.data.defaultBranch").value(false));

        verify(branchLoadUseCase).loadBranch(1L, "feature", 7L);
    }

    @Test
    void deleteBranch_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/1/branches/feature"))
                .andExpect(status().isNoContent());

        verify(branchManagementUseCase).deleteBranch(7L, 1L, "feature");
    }

    /** The malformed subjects the identity resolver must refuse. Zero is malformed, not absent. */

    /**
     * Rejects an unauthenticated caller with AUTH-001 and no use case interaction.
     *
     * <p>This helper used to loop over malformed principal names first -- "0", "00", "-1", "+1",
     * " 42", "42 ", "abc", an overflowing digit string -- and assert each was refused here. None of
     * them is expressible any more: the principal is an {@code AuthenticatedUser} carrying a positive
     * Long, so a malformed requester cannot reach a controller to be rejected by one.
     *
     * <p>The rule moved rather than vanished. {@code JwtTokenCodec.parseSubject} rejects the same
     * spellings against the token itself, and {@code JwtTokenCodecTest} asserts twenty-five of them.
     * That is the better location: the codec sits on the path of every authenticated request, while
     * this controller was one of twelve, and three of the twelve used a laxer resolver that accepted
     * "0" outright.
     */
    private void assertRejectedWithAuth001(
            java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> request)
            throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(request.get())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
    }


    @Test
    void create_rejectsAnonymousWithAuth001AndNoWrite() throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchName\":\"feature\",\"sourceBranch\":\"main\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        verifyNoInteractions(branchManagementUseCase);
    }

    @Test
    void create_rejectsNonMemberWithoutWrite() throws Exception {
        when(branchRequestMapper.toCommand(anyLong(), anyLong(), any(BranchCreateRequest.class)))
                .thenReturn(new BranchCreateCommand(7L, 1L, "feature", "main", false));
        doThrow(new ApplicationException(ApplicationErrorCode.ACCESS_DENIED,
                "Insufficient permission to commit to repository: team/repo"))
                .when(branchManagementUseCase).createBranch(any());

        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchName\":\"feature\",\"sourceBranch\":\"main\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_returnsNotFound() throws Exception {
        when(branchRequestMapper.toCommand(anyLong(), anyLong(), any(BranchCreateRequest.class)))
                .thenReturn(new BranchCreateCommand(7L, 1L, "feature", "main", false));
        doThrow(new RepositoryNotFoundException(1L)).when(branchManagementUseCase).createBranch(any());

        mockMvc.perform(post("/api/repositories/1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchName\":\"feature\",\"sourceBranch\":\"main\"}"))
                .andExpect(status().isNotFound());
    }


    @Test
    void deleteBranch_rejectsAnonymousWithAuth001AndNoWrite() throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(delete("/api/repositories/1/branches/feature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        verifyNoInteractions(branchManagementUseCase);
    }

    @Test
    void deleteBranch_rejectsNonMemberWithoutWrite() throws Exception {
        doThrow(new ApplicationException(ApplicationErrorCode.ACCESS_DENIED,
                "Insufficient permission to commit to repository: team/repo"))
                .when(branchManagementUseCase).deleteBranch(7L, 1L, "feature");

        mockMvc.perform(delete("/api/repositories/1/branches/feature"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteBranch_returnsNotFound() throws Exception {
        doThrow(new RepositoryNotFoundException(1L))
                .when(branchManagementUseCase).deleteBranch(7L, 1L, "feature");

        mockMvc.perform(delete("/api/repositories/1/branches/feature"))
                .andExpect(status().isNotFound());
    }
}
