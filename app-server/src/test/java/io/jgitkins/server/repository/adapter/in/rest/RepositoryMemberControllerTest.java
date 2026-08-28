package io.jgitkins.server.repository.adapter.in.rest;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import io.jgitkins.server.repository.application.exception.RepositoryNotFoundException;
import io.jgitkins.server.repository.application.exception.RepositoryAccessDeniedException;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import io.jgitkins.server.support.TestAuthentication;
import org.springframework.context.annotation.Import;
import io.jgitkins.server.identity.access.adapter.in.support.RequesterUserIdResolver;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.repository.application.contract.result.RepositoryMemberSummary;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberLoadUseCase;
import io.jgitkins.server.repository.application.port.in.RepositoryMemberManagementUseCase;
import io.jgitkins.server.repository.domain.vo.RepositoryMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RepositoryMemberController.class)
@Import({RequesterUserIdResolver.class, ErrorStatusMappingTestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class RepositoryMemberControllerTest {

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
    private RepositoryMemberManagementUseCase repositoryMemberManagementUseCase;

    @MockBean
    private RepositoryMemberLoadUseCase repositoryMemberLoadUseCase;

    @Test
    void addMember_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 2,
                "role", "WRITER"
        ));

        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(repositoryMemberManagementUseCase).addRepositoryMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.repositoryId().equals(1L)
                        && cmd.userId().equals(2L)
                        && cmd.role() == RepositoryMemberRole.WRITER
        ));
    }

    @Test
    void addMember_allowsMissingRoleAndPassesNullRole() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 3
        ));

        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(repositoryMemberManagementUseCase).addRepositoryMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.repositoryId().equals(1L)
                        && cmd.userId().equals(3L)
                        && cmd.role() == null
        ));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/repositories/1/members/2"))
                .andExpect(status().isNoContent());

        verify(repositoryMemberManagementUseCase).removeRepositoryMember(7L, 1L, 2L);
    }

    @Test
    void listMembers_returnsMemberSummaries() throws Exception {
        when(repositoryMemberLoadUseCase.getRepositoryMembers(7L, 1L)).thenReturn(List.of(
                new RepositoryMemberSummary(2L, RepositoryMemberRole.MAINTAINER, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        mockMvc.perform(get("/api/repositories/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2L))
                .andExpect(jsonPath("$.data[0].role").value("MAINTAINER"));

        verify(repositoryMemberLoadUseCase).getRepositoryMembers(7L, 1L);
    }

    /** The malformed subjects the identity resolver must refuse. Zero is malformed, not absent. */
    private static final java.util.List<String> MALFORMED_SUBJECTS =
            java.util.List.of("0", "00", "-1", "+1", " 7", "7 ", "abc", "9999999999999999999999");

    private void assertRejectedWithAuth001(
            java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> request)
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
    void addMember_rejectsMalformedPrincipalWithAuth001AndNoWrite() throws Exception {
        String body = "{\"userId\":2,\"role\":\"MAINTAINER\"}";
        assertRejectedWithAuth001(() -> post("/api/repositories/1/members")
                .contentType(MediaType.APPLICATION_JSON).content(body));
        verifyNoInteractions(repositoryMemberManagementUseCase);
    }

    @Test
    void addMember_rejectsAnonymousWithAuth001AndNoWrite() throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"role\":\"MAINTAINER\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        verifyNoInteractions(repositoryMemberManagementUseCase);
    }

    @Test
    void addMember_rejectsNonOwnerWithoutWrite() throws Exception {
        // The authorization this task introduced: before it, any authenticated caller who knew a
        // repository id could add themselves as a member.
        doThrow(new RepositoryAccessDeniedException("Repository member management is not allowed"))
                .when(repositoryMemberManagementUseCase).addRepositoryMember(any());

        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"role\":\"MAINTAINER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMember_returnsNotFound() throws Exception {
        doThrow(new RepositoryNotFoundException(1L))
                .when(repositoryMemberManagementUseCase).addRepositoryMember(any());

        mockMvc.perform(post("/api/repositories/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"role\":\"MAINTAINER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeMember_rejectsMalformedPrincipalWithAuth001AndNoWrite() throws Exception {
        assertRejectedWithAuth001(() -> delete("/api/repositories/1/members/2"));
        verifyNoInteractions(repositoryMemberManagementUseCase);
    }

    @Test
    void removeMember_rejectsAnonymousWithAuth001AndNoWrite() throws Exception {
        TestAuthentication.clear();
        mockMvc.perform(delete("/api/repositories/1/members/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));
        verifyNoInteractions(repositoryMemberManagementUseCase);
    }

    @Test
    void removeMember_rejectsNonOwnerWithoutWrite() throws Exception {
        doThrow(new RepositoryAccessDeniedException("Repository member management is not allowed"))
                .when(repositoryMemberManagementUseCase).removeRepositoryMember(7L, 1L, 2L);

        mockMvc.perform(delete("/api/repositories/1/members/2")).andExpect(status().isForbidden());
    }

    @Test
    void removeMember_returnsNotFound() throws Exception {
        doThrow(new RepositoryNotFoundException(1L))
                .when(repositoryMemberManagementUseCase).removeRepositoryMember(7L, 1L, 2L);

        mockMvc.perform(delete("/api/repositories/1/members/2")).andExpect(status().isNotFound());
    }

    @Test
    void listMembers_returnsSummariesForAuthorizedMember() throws Exception {
        when(repositoryMemberLoadUseCase.getRepositoryMembers(7L, 1L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/repositories/1/members")).andExpect(status().isOk());

        verify(repositoryMemberLoadUseCase).getRepositoryMembers(7L, 1L);
    }

    @Test
    void listMembers_deniesBeforeMemberQuery() throws Exception {
        when(repositoryMemberLoadUseCase.getRepositoryMembers(7L, 1L))
                .thenThrow(new RepositoryAccessDeniedException("Repository member management is not allowed"));

        mockMvc.perform(get("/api/repositories/1/members")).andExpect(status().isForbidden());
    }

    @Test
    void listMembers_returns404ForMissingRepository() throws Exception {
        when(repositoryMemberLoadUseCase.getRepositoryMembers(7L, 1L))
                .thenThrow(new RepositoryNotFoundException(1L));

        mockMvc.perform(get("/api/repositories/1/members")).andExpect(status().isNotFound());
    }

    @Test
    void listMembers_rejectsMalformedPrincipal() throws Exception {
        assertRejectedWithAuth001(() -> get("/api/repositories/1/members"));
        // A member list is never public, so unlike the repository reads this route rejects rather than
        // narrowing -- and the use case is never reached, so nothing can be inferred from a response time.
        verifyNoInteractions(repositoryMemberLoadUseCase);
    }
}
