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
import io.jgitkins.server.collaboration.application.dto.result.OrganizeMemberSummary;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberAddUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberQueryUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeMemberRemoveUseCase;
import io.jgitkins.server.collaboration.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeMemberController;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeMemberAddRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeMemberRequestMapper;
import io.jgitkins.server.collaboration.application.dto.command.OrganizeMemberAddCommand;
import io.jgitkins.server.collaboration.domain.vo.OrganizeMemberRole;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizeMemberController.class)
@AutoConfigureMockMvc
class OrganizeMemberControllerTest {

    @BeforeEach
    void authenticate() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User("7", "", java.util.List.of()), "", java.util.List.of()));
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

    @MockBean
    private RequesterUserIdResolver requesterUserIdResolver;

    @Test
    void addMember_returnsOk() throws Exception {
        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
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
        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
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
        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
        mockMvc.perform(delete("/api/organizes/1/members/2").with(user("7")))
                .andExpect(status().isNoContent());

        verify(organizeMemberRemoveUseCase).removeOrganizeMember(1L, 7L, 2L);
    }

    @Test
    void addMember_missingRequesterReturnsRaw401ApplicationError() throws Exception {
        when(requesterUserIdResolver.resolve(null)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/organizes/1/members")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                        new io.jgitkins.server.collaboration.adapter.in.rest.OrganizeMemberControllerTest.NullUsernamePrincipal(), null, java.util.List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2,"role":"MEMBER"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"))
                .andExpect(jsonPath("$.error.source").value("application"));
        verifyNoInteractions(organizeMemberAddUseCase, organizeMemberRequestMapper);
    }

    @Test
    void removeMember_missingRequesterReturnsRaw401ApplicationError() throws Exception {
        when(requesterUserIdResolver.resolve(null)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(delete("/api/organizes/1/members/2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
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
