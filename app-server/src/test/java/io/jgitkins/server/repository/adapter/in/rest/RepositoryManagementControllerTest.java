package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import io.jgitkins.server.support.TestAuthentication;
import org.springframework.context.annotation.Import;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
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
import io.jgitkins.server.repository.application.contract.command.RepositoryCreateCommand;
import io.jgitkins.server.repository.application.contract.result.RepositoryResult;
import io.jgitkins.server.repository.application.port.in.RepositoryLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryManagementUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryOverviewUseCase;
import io.jgitkins.server.shared.domain.model.vo.OwnerType;
import io.jgitkins.server.repository.adapter.in.rest.dto.request.RepositoryCreateRequest;
import io.jgitkins.server.repository.adapter.in.rest.mapper.RepositoryRequestMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryManagementController.class)
@Import(RequesterUserIdResolver.class)
@AutoConfigureMockMvc(addFilters = false)
class RepositoryManagementControllerTest {

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
    private RepositoryManagementUseCase repositoryManagementUseCase;

    @MockBean
    private RepositoryLoadUseCase repositoryLoadUseCase;

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

        when(repositoryRequestMapper.toCommand(anyLong(), any(RepositoryCreateRequest.class))).thenReturn(command);
        when(repositoryManagementUseCase.create(command)).thenReturn(result);

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

        verify(repositoryManagementUseCase).create(command);
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

        verify(repositoryManagementUseCase).deleteRepository(7L, 9L);
    }

    /** The malformed subjects the identity resolver must refuse. Zero is malformed, not absent. */
    private static final java.util.List<String> MALFORMED_SUBJECTS =
            java.util.List.of("0", "00", "-1", "+1", " 7", "7 ", "abc", "9999999999999999999999");

    private void assertRejectedWithAuth001(java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> request)
            throws Exception {
        for (String malformed : MALFORMED_SUBJECTS) {
            TestAuthentication.authenticateAs(malformed);
            mockMvc.perform(request.get())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        }
        TestAuthentication.clear();
        mockMvc.perform(request.get())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
    }

    @Test
    void create_rejectsMalformedPrincipalWithAuth001AndNoUseCase() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "repoName", "sample-repo", "ownerType", "USER", "mainBranch", "main"));
        for (String malformed : MALFORMED_SUBJECTS) {
            TestAuthentication.authenticateAs(malformed);
            mockMvc.perform(post("/api/repositories")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        }
        // The point of resolving at the adapter: a broken credential never reaches the use case, so it
        // cannot cause a repository row, a git directory, or an audit entry.
        verifyNoInteractions(repositoryManagementUseCase);
    }

    @Test
    void create_rejectsAnonymousWithAuth001AndNoUseCase() throws Exception {
        TestAuthentication.clear();
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "repoName", "sample-repo", "ownerType", "USER", "mainBranch", "main"));
        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        verifyNoInteractions(repositoryManagementUseCase);
    }

    @Test
    void create_rejectsNonOwnerWithoutWrite() throws Exception {
        // Authorization itself lives in the application layer; what this asserts is that its refusal
        // still reaches the client as the same 403 envelope after the actor moved to an argument.
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "repoName", "sample-repo", "ownerType", "ORGANIZATION", "organizeId", 10,
                "mainBranch", "main"));
        when(repositoryRequestMapper.toCommand(anyLong(), any(RepositoryCreateRequest.class)))
                .thenReturn(RepositoryCreateCommand.builder().requesterUserId(7L)
                        .repoName("sample-repo").ownerType(OwnerType.ORGANIZATION).organizeId(10L)
                        .mainBranch("main").build());
        when(repositoryManagementUseCase.create(any()))
                .thenThrow(new RepositoryAccessDeniedException("User is not a member of the organization."));

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_returnsNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "repoName", "sample-repo", "ownerType", "USER", "mainBranch", "main"));
        when(repositoryRequestMapper.toCommand(anyLong(), any(RepositoryCreateRequest.class)))
                .thenReturn(RepositoryCreateCommand.builder().requesterUserId(7L)
                        .repoName("sample-repo").ownerType(OwnerType.USER).mainBranch("main").build());
        when(repositoryManagementUseCase.create(any())).thenThrow(new RepositoryNotFoundException(1L));

        mockMvc.perform(post("/api/repositories")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRepository_rejectsMalformedPrincipalWithAuth001AndNoUseCase() throws Exception {
        assertRejectedWithAuth001(() -> delete("/api/repositories/1"));
        verifyNoInteractions(repositoryManagementUseCase);
    }

    @Test
    void deleteRepository_rejectsAnonymousWithAuth001AndNoUseCase() throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(delete("/api/repositories/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        verifyNoInteractions(repositoryManagementUseCase);
    }

    @Test
    void deleteRepository_rejectsNonOwnerWithoutDelete() throws Exception {
        doThrow(new RepositoryAccessDeniedException("Cannot delete another user's repository"))
                .when(repositoryManagementUseCase).deleteRepository(7L, 1L);

        mockMvc.perform(delete("/api/repositories/1")).andExpect(status().isForbidden());
    }

    @Test
    void deleteRepository_returnsNotFound() throws Exception {
        doThrow(new RepositoryNotFoundException(1L))
                .when(repositoryManagementUseCase).deleteRepository(7L, 1L);

        mockMvc.perform(delete("/api/repositories/1")).andExpect(status().isNotFound());
    }
}
