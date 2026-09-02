package io.jgitkins.server.collaboration.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.collaboration.application.contract.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeMemberManagementController;
import io.jgitkins.server.collaboration.adapter.in.rest.contract.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.translator.OrganizeMemberRequestMapper;
import io.jgitkins.server.collaboration.application.contract.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.support.PermissiveSliceSecurityConfig;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizeMemberManagementController.class)
// addFilters = false because this is a controller slice test: it authenticates by seeding
// SecurityContextHolder, which JwtAuthenticationFilter now clears on a request without a Bearer
// header. The real chain is covered by AnonymousPrincipalResolutionTest and
// OAuth2SessionPrincipalResolutionTest; the other eight controller slice tests already do this.
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, PermissiveSliceSecurityConfig.class,
        ErrorStatusMappingTestConfig.class})
class OrganizeMemberManagementControllerTest {

    @BeforeEach
    void authenticate() {
        io.jgitkins.server.support.TestAuthentication.authenticateAs(7L);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizeMemberAddUseCase organizeMemberAddUseCase;

    @MockBean
    private OrganizeMemberQueryUseCase organizeMemberQueryUseCase;

    @MockBean
    private OrganizeMemberRemoveUseCase organizeMemberRemoveUseCase;

    @MockBean
    private OrganizeMemberRequestMapper organizeMemberRequestMapper;


    @Test
    void addMember_returnsOk() throws Exception {
        when(organizeMemberRequestMapper.toCommand(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(OrganizeMemberAddRequest.class), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(new OrganizeMemberAddCommand(1L, 2L, OrganizeMemberRole.MEMBER, 7L));
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 2,
                "role", "MEMBER"
        ));

        mockMvc.perform(post("/api/organizes/1/members")
                        .with(user("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(organizeMemberAddUseCase).addOrganizeMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.organizeId().equals(1L)
                        && cmd.userId().equals(2L)
                        && cmd.role() == OrganizeMemberRole.MEMBER
        ));
    }

    @Test
    void addMember_allowsMissingRoleAndPassesNullRole() throws Exception {
        when(organizeMemberRequestMapper.toCommand(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(OrganizeMemberAddRequest.class), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(new OrganizeMemberAddCommand(1L, 3L, null, 7L));
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", 3
        ));

        mockMvc.perform(post("/api/organizes/1/members")
                        .with(user("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(organizeMemberAddUseCase).addOrganizeMember(org.mockito.ArgumentMatchers.argThat(cmd ->
                cmd.organizeId().equals(1L)
                        && cmd.userId().equals(3L)
                        && cmd.role() == null
        ));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/organizes/1/members/2").with(user("7")))
                .andExpect(status().isNoContent());

        verify(organizeMemberRemoveUseCase).removeOrganizeMember(1L, 7L, 2L);
    }

    @Test
    void addMember_missingRequesterReturns401() throws Exception {
        io.jgitkins.server.support.TestAuthentication.clear();

        mockMvc.perform(post("/api/organizes/1/members")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                        new io.jgitkins.server.collaboration.adapter.in.rest.OrganizeMemberManagementControllerTest.NullUsernamePrincipal(), null, java.util.List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"role":"MEMBER"}
                                """))
                .andExpect(status().isUnauthorized())
                // Task 2.91. The status was already 401 and stays 401; the code changes from the raw
                // ApplicationErrorCode name to the problem spec's AUTH-001, which is what the other four
                // controllers throwing UnauthenticatedException already return. The spec for this task
                // claimed consolidating the two exception types was wire-identical. It is not: the
                // status matches, the body's code does not, and these two tests were named "Raw"
                // because their author was pinning exactly that difference.
                .andExpect(jsonPath("$.error.code").value("AUTH-001"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"))
                .andExpect(jsonPath("$.error.source").value("application"));
        verifyNoInteractions(organizeMemberAddUseCase, organizeMemberRequestMapper);
    }

    @Test
    void removeMember_missingRequesterReturns401() throws Exception {
        io.jgitkins.server.support.TestAuthentication.clear();

        mockMvc.perform(delete("/api/organizes/1/members/2"))
                .andExpect(status().isUnauthorized())
                // Task 2.91. The status was already 401 and stays 401; the code changes from the raw
                // ApplicationErrorCode name to the problem spec's AUTH-001, which is what the other four
                // controllers throwing UnauthenticatedException already return. The spec for this task
                // claimed consolidating the two exception types was wire-identical. It is not: the
                // status matches, the body's code does not, and these two tests were named "Raw"
                // because their author was pinning exactly that difference.
                .andExpect(jsonPath("$.error.code").value("AUTH-001"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"))
                .andExpect(jsonPath("$.error.source").value("application"));
        verifyNoInteractions(organizeMemberRemoveUseCase);
    }

    @Test
    void listMembers_returnsMemberSummaries() throws Exception {
        when(organizeMemberQueryUseCase.getOrganizeMembers(1L)).thenReturn(List.of(
                new OrganizeMemberSummary(2L, OrganizeMemberRole.MEMBER, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        mockMvc.perform(get("/api/organizes/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(2L))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"));

        verify(organizeMemberQueryUseCase).getOrganizeMembers(1L);
    }

    private static final class NullUsernamePrincipal {
        public String getUsername() {
            return null;
        }
    }
}
