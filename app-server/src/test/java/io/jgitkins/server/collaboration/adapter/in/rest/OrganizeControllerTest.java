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
import io.jgitkins.server.collaboration.adapter.in.rest.OrganizeController;
import io.jgitkins.server.collaboration.adapter.in.rest.dto.request.OrganizeCreationRequest;
import io.jgitkins.server.collaboration.adapter.in.rest.mapper.OrganizeRequestMapper;
import io.jgitkins.server.collaboration.adapter.in.support.RequesterUserIdResolver;
import io.jgitkins.server.common.presentation.advice.GlobalExceptionHandler;
import io.jgitkins.server.support.PermissiveSliceSecurityConfig;
import io.jgitkins.server.common.presentation.advice.mapper.CompositeErrorHttpStatusMapper;
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

@WebMvcTest(OrganizeController.class)
// addFilters = false because this is a controller slice test: it authenticates by seeding
// SecurityContextHolder, which JwtAuthenticationFilter now clears on a request without a Bearer
// header. The real chain is covered by AnonymousPrincipalResolutionTest and
// OAuth2SessionPrincipalResolutionTest; the other eight controller slice tests already do this.
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "7")
@Import({GlobalExceptionHandler.class, PermissiveSliceSecurityConfig.class})
class OrganizeControllerTest {

    @BeforeEach
    void authenticate() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User("7", "", java.util.List.of()), "", java.util.List.of()));
        when(statusMapper.map(org.mockito.ArgumentMatchers.any())).thenReturn(org.springframework.http.HttpStatus.FORBIDDEN);
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

    @MockBean
    private RequesterUserIdResolver requesterUserIdResolver;

    @MockBean
    private CompositeErrorHttpStatusMapper statusMapper;

    @Test
    void createOrganize_returnsCreatedResponse() throws Exception {
        OrganizeCreationCommand command = new OrganizeCreationCommand("core-team", "Core Team", 7L);
        OrganizeCreationResult result = new OrganizeCreationResult(10L, "core-team", "Core Team", 1L, null, null);

        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
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

        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
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
    void createOrganize_nullRequesterReturnsOrg403ApplicationError() throws Exception {
        when(requesterUserIdResolver.resolve(null)).thenReturn(java.util.Optional.empty());
        when(statusMapper.map(org.mockito.ArgumentMatchers.any())).thenReturn(org.springframework.http.HttpStatus.FORBIDDEN);
        assertCreateDenied(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new NullUsernamePrincipal(), null, java.util.List.of()), null);
    }

    @Test
    void createOrganize_blankRequesterReturnsOrg403ApplicationError() throws Exception {
        when(requesterUserIdResolver.resolve(" ")).thenReturn(java.util.Optional.empty());
        assertCreateDenied(tokenFor(" "), " ");
    }

    @Test
    void createOrganize_nonnumericRequesterReturnsOrg403ApplicationError() throws Exception {
        when(requesterUserIdResolver.resolve("not-numeric")).thenReturn(java.util.Optional.empty());
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORG-403"))
                .andExpect(jsonPath("$.error.message").value("An authenticated user is required"))
                .andExpect(jsonPath("$.error.source").value("application"));
        org.mockito.Mockito.verify(requesterUserIdResolver).resolve(subject);
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
        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
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
        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.empty());
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
    void deleteOrganize_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/organizes/9"))
                .andExpect(status().isNoContent());

        verify(organizeDeletionUseCase).deleteOrganize(9L);
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
