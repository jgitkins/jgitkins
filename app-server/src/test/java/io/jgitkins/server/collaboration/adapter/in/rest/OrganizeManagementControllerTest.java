package io.jgitkins.server.collaboration.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.collaboration.application.dto.command.OrganizeCreationCommand;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.port.in.OrganizeCreationUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeDeletionUseCase;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeManagementController;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeRequestMapper;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.support.PermissiveSliceSecurityConfig;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizeManagementController.class)
// addFilters = false because this is a controller slice test: it authenticates by seeding
// SecurityContextHolder, which JwtAuthenticationFilter now clears on a request without a Bearer
// header. The real chain is covered by AnonymousPrincipalResolutionTest and
// OAuth2SessionPrincipalResolutionTest; the other eight controller slice tests already do this.
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "7")
@Import({GlobalExceptionHandler.class, PermissiveSliceSecurityConfig.class,
        ErrorStatusMappingTestConfig.class})
class OrganizeManagementControllerTest {

    @BeforeEach
    void authenticate() {
        io.jgitkins.server.support.TestAuthentication.authenticateAs(7L);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizeCreationUseCase organizeCreationUseCase;

    @MockBean
    private OrganizeLoadUseCase organizeLoadUseCase;

    @MockBean
    private OrganizeDeletionUseCase organizeDeletionUseCase;

    @MockBean
    private OrganizeRequestMapper organizeRequestMapper;


    @Test
    void createOrganize_returnsCreatedResponse() throws Exception {
        OrganizeCreationCommand command = new OrganizeCreationCommand("core-team", "Core Team", 7L);
        OrganizeCreationResult result = new OrganizeCreationResult(10L, "core-team", "Core Team", 1L, null, null);

        when(organizeRequestMapper.toCommand(org.mockito.ArgumentMatchers.any(OrganizeCreationRequest.class), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(command);
        when(organizeCreationUseCase.createOrganize(command)).thenReturn(result);

        mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "core-team",
                                "description", "Core Team"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/organizes/10")))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.name").value("core-team"));

        verify(organizeCreationUseCase).createOrganize(command);
    }

    @Test
    void createOrganize_ignoresLegacyOwnerIdAndKeepsAuthenticatedOwnerResult() throws Exception {
        OrganizeCreationCommand command = new OrganizeCreationCommand("core-team", "Core Team", 7L);
        OrganizeCreationResult result = new OrganizeCreationResult(10L, "core-team", "Core Team", 7L, null, null);

        when(organizeRequestMapper.toCommand(org.mockito.ArgumentMatchers.any(OrganizeCreationRequest.class), org.mockito.ArgumentMatchers.eq(7L)))
                .thenReturn(command);
        when(organizeCreationUseCase.createOrganize(command)).thenReturn(result);

        mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"core-team","description":"Core Team","ownerId":999}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownerId").value(7L));

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(OrganizeCreationRequest.class);
        verify(organizeRequestMapper).toCommand(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq(7L));
        assertThat(requestCaptor.getValue().name()).isEqualTo("core-team");
        assertThat(requestCaptor.getValue().description()).isEqualTo("Core Team");
        assertThat(java.util.Arrays.stream(OrganizeCreationRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList()).doesNotContain("ownerId");
    }

    @Test
    void createOrganize_nullRequesterReturns401() throws Exception {
        assertCreateDenied(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new NullUsernamePrincipal(), null, java.util.List.of()), null);
    }

    @Test
    void createOrganize_blankRequesterReturns401() throws Exception {
        assertCreateDenied(tokenFor(" "), " ");
    }

    @Test
    void createOrganize_nonnumericRequesterReturns401() throws Exception {
        assertCreateDenied(tokenFor("not-numeric"), "not-numeric");
    }

    /**
     * Seeds the context directly rather than through a {@code SecurityMockMvcRequestPostProcessors}
     * post-processor. Those install the context via the security filter chain, which this slice test
     * no longer runs, so a post-processor would silently leave the {@code @BeforeEach} subject in place
     * and the assertion below would pass for the wrong reason.
     */
    private void assertCreateDenied(org.springframework.security.core.Authentication auth,
                                    String subject) throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        mockMvc.perform(post("/api/organizes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"core-team\",\"description\":\"Core Team\"}"))
                // Task 2.91 changed this on purpose. A caller with no credentials was answered 403
                // ORG-403, which says "you are not allowed", when the truth is "I do not know who you
                // are". RFC 9110 puts that at 401, the apiAnauthorizeHandler wired into this same chain
                // answers 401, and the other five controllers answer 401. This one disagreed with all
                // three.
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"))
                .andExpect(jsonPath("$.error.message").value("An authenticated user is required"))
                .andExpect(jsonPath("$.error.source").value("application"));
        // The resolver that used to be verified here is gone: the principal arrives typed, so there
        // is no name-to-number step left to assert. What matters is unchanged and asserted above --
        // an anonymous caller is refused and the use case is never reached.
        org.mockito.Mockito.verifyNoInteractions(organizeCreationUseCase, organizeRequestMapper);
    }

    @Test
    void getOrganizes_returnsList() throws Exception {
        when(organizeLoadUseCase.getOrganizes()).thenReturn(List.of(
                new OrganizeCreationResult(1L, "org-a", null, null, null, null),
                new OrganizeCreationResult(2L, "org-b", null, null, null, null)
        ));

        mockMvc.perform(get("/api/organizes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("org-a"))
                .andExpect(jsonPath("$.data[1].name").value("org-b"));

        verify(organizeLoadUseCase).getOrganizes();
    }

    @Test
    void getAccessibleOrganizes_returnsList() throws Exception {
        when(organizeLoadUseCase.getAccessibleOrganizes(7L)).thenReturn(List.of(
                new OrganizeCreationResult(3L, "org-c", null, null, null, null)
        ));

        mockMvc.perform(get("/api/organizes/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3L));

        verify(organizeLoadUseCase).getAccessibleOrganizes(7L);
    }

    @Test
    void getAccessibleOrganizes_withoutRequesterReturnsOkEmptyList() throws Exception {
        io.jgitkins.server.support.TestAuthentication.clear();
        when(organizeLoadUseCase.getAccessibleOrganizes(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/organizes/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(organizeLoadUseCase).getAccessibleOrganizes(null);
    }

    @Test
    void getOrganize_returnsSingleResult() throws Exception {
        when(organizeLoadUseCase.getOrganize(7L)).thenReturn(
                new OrganizeCreationResult(7L, "org-seven", null, null, null, null)
        );

        mockMvc.perform(get("/api/organizes/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7L))
                .andExpect(jsonPath("$.data.name").value("org-seven"));

        verify(organizeLoadUseCase).getOrganize(7L);
    }

    @Test
    void deleteOrganize_returnsNoContentAndPassesTheRequester() throws Exception {

        mockMvc.perform(delete("/api/organizes/9"))
                .andExpect(status().isNoContent());

        // The exact requester, not any(). Before task 2.111 the use case took no actor at all.
        verify(organizeDeletionUseCase).deleteOrganize(7L, 9L);
    }

    @Test
    void deleteOrganize_rejectsAnAnonymousCallerWithoutReachingTheUseCase() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        mockMvc.perform(delete("/api/organizes/9"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH-001"));

        org.mockito.Mockito.verifyNoInteractions(organizeDeletionUseCase);
    }

    private static org.springframework.security.core.Authentication tokenFor(String subject) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(subject, "", java.util.List.of()),
                "", java.util.List.of());
    }

    private static final class NullUsernamePrincipal {
        public String getUsername() {
            return null;
        }
    }

    private static org.springframework.security.core.Authentication authenticatedUser() {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User("7", "", java.util.List.of()), "", java.util.List.of());
    }

    private static org.springframework.security.core.context.SecurityContext authenticatedSecurityContext() {
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticatedUser());
        return context;
    }
}
