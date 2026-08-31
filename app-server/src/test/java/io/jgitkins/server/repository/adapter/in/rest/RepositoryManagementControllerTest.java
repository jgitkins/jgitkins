package io.jgitkins.server.repository.adapter.in.rest;

import io.jgitkins.server.shared.application.exception.ApplicationException;
import io.jgitkins.server.shared.application.error.ApplicationErrorCode;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
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
@Import({ErrorStatusMappingTestConfig.class})
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
        when(repositoryLoadUseCase.loadRepository(7L, 1L))
                .thenReturn(new RepositoryResult(1L, null, "repo-1", null, null, null, null, null, null, null, null, false, null, null, null));

        mockMvc.perform(get("/api/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("repo-1"));

        verify(repositoryLoadUseCase).loadRepository(7L, 1L);
    }

    @Test
    void getRepositories_returnsList() throws Exception {
        when(repositoryLoadUseCase.loadRepositories(7L)).thenReturn(List.of(
                new RepositoryResult(1L, null, "repo-1", null, null, null, null, null, null, null, null, false, null, null, null),
                new RepositoryResult(2L, null, "repo-2", null, null, null, null, null, null, null, null, false, null, null, null)
        ));

        mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("repo-1"))
                .andExpect(jsonPath("$.data[1].name").value("repo-2"));

        verify(repositoryLoadUseCase).loadRepositories(7L);
    }

    @Test
    void getUserRepositories_returnsList() throws Exception {
        when(repositoryLoadUseCase.loadUserRepositories(7L, "alice"))
                .thenReturn(List.of(new RepositoryResult(3L, null, "alice-repo", null, null, null, null, null, null, null, null, false, null, null, null)));

        mockMvc.perform(get("/api/repositories/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3L))
                .andExpect(jsonPath("$.data[0].name").value("alice-repo"));

        verify(repositoryLoadUseCase).loadUserRepositories(7L, "alice");
    }

    @Test
    void deleteRepository_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/9"))
                .andExpect(status().isNoContent());

        verify(repositoryManagementUseCase).deleteRepository(7L, 9L);
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
    private void assertRejectedWithAuth001(java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> request)
            throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(request.get())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
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

    @Test
    void getRepository_returnsMetadataForPublicAnonymous() throws Exception {
        TestAuthentication.clear();
        when(repositoryLoadUseCase.loadRepository(null, 1L)).thenReturn(publicRepository());

        mockMvc.perform(get("/api/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));

        // Anonymous reaches the use case as null rather than being rejected at the adapter. A public
        // repository is readable without a caller, and demanding one here would break every anonymous
        // read while looking like a security improvement.
        verify(repositoryLoadUseCase).loadRepository(null, 1L);
    }

    @Test
    void getRepository_returnsMetadataForPrivateOwner() throws Exception {
        when(repositoryLoadUseCase.loadRepository(7L, 1L)).thenReturn(privateRepository());

        mockMvc.perform(get("/api/repositories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));

        verify(repositoryLoadUseCase).loadRepository(7L, 1L);
    }

    @Test
    void getRepository_deniesPrivateNonMember() throws Exception {
        when(repositoryLoadUseCase.loadRepository(7L, 1L)).thenThrow(new ApplicationException(
                ApplicationErrorCode.ACCESS_DENIED, "Insufficient permission to access repository: repo"));

        mockMvc.perform(get("/api/repositories/1")).andExpect(status().isForbidden());
    }

    @Test
    void getRepository_returns404ForMissingRepository() throws Exception {
        when(repositoryLoadUseCase.loadRepository(7L, 1L)).thenThrow(new RepositoryNotFoundException(1L));

        mockMvc.perform(get("/api/repositories/1")).andExpect(status().isNotFound());
    }

    @Test
    void getRepositories_preservesPublicAnonymousRead() throws Exception {
        TestAuthentication.clear();
        when(repositoryLoadUseCase.loadRepositories(null)).thenReturn(java.util.List.of(publicRepository()));

        mockMvc.perform(get("/api/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));

        // The list narrows by visibility rather than failing. The requester is the filter's input, so a
        // null one must reach the query instead of being turned into a rejection.
        verify(repositoryLoadUseCase).loadRepositories(null);
    }

    @Test
    void getUserRepositories_passesExplicitRequester() throws Exception {
        when(repositoryLoadUseCase.loadUserRepositories(7L, "alice"))
                .thenReturn(java.util.List.of(privateRepository()));

        mockMvc.perform(get("/api/repositories/users/alice")).andExpect(status().isOk());

        verify(repositoryLoadUseCase).loadUserRepositories(7L, "alice");
    }

    @Test
    void getOverview_passesExplicitRequester() throws Exception {
        when(repositoryOverviewUseCase.getOverview(7L, 1L, "main")).thenReturn(
                new io.jgitkins.server.repository.application.contract.result.RepositoryOverviewResult(
                        privateRepository(), java.util.List.of(), java.util.List.of(), "main", "OWNER", true));

        mockMvc.perform(get("/api/repositories/1/overview").param("branch", "main"))
                .andExpect(status().isOk());

        verify(repositoryOverviewUseCase).getOverview(7L, 1L, "main");
    }

    private static RepositoryResult publicRepository() {
        return new RepositoryResult(1L, "USER", "open", "org/open", "main", "PUBLIC",
                null, 9L, null, "/org/open.git", null, false, null, null, null);
    }

    private static RepositoryResult privateRepository() {
        return new RepositoryResult(1L, "USER", "repo", "org/repo", "main", "PRIVATE",
                null, 7L, null, "/org/repo.git", null, false, null, null, null);
    }
}
