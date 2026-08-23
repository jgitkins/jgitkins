package io.jgitkins.server.collaboration.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.collaboration.application.dto.result.OrganizeCreationResult;
import io.jgitkins.server.collaboration.application.port.in.OrganizeLoadUseCase;
import io.jgitkins.server.collaboration.adapter.in.support.RequesterUserIdResolver;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;

@ExtendWith(MockitoExtension.class)
@WithMockUser(username = "7")
class WebOrganizeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrganizeLoadUseCase organizeLoadUseCase;

    @Mock
    private RequesterUserIdResolver requesterUserIdResolver;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User("7", "", java.util.List.of()), "", java.util.List.of()));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new WebOrganizeController(organizeLoadUseCase, requesterUserIdResolver)
        ).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @Test
    void getAccessibleOrganizes_preservesApplicationResultPayload() throws Exception {
        when(requesterUserIdResolver.resolve("7")).thenReturn(java.util.Optional.of(7L));
                when(organizeLoadUseCase.getAccessibleOrganizes(7L)).thenReturn(List.of(
                new OrganizeCreationResult(3L, "org-c", "description", 7L, null, null)
        ));

        mockMvc.perform(get("/api/internal/organizes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3L))
                .andExpect(jsonPath("$.data[0].name").value("org-c"))
                .andExpect(jsonPath("$.data[0].description").value("description"))
                .andExpect(jsonPath("$.data[0].ownerId").value(7L))
                .andExpect(jsonPath("$.data[0].createdAt").value((Object) null))
                .andExpect(jsonPath("$.data[0].updatedAt").value((Object) null));

        org.mockito.Mockito.verify(organizeLoadUseCase).getAccessibleOrganizes(7L);
    }

    @Test
    void getAccessibleOrganizes_withoutSubject_passesNullAndReturnsEmpty() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        when(requesterUserIdResolver.resolve(null)).thenReturn(java.util.Optional.empty());
        when(organizeLoadUseCase.getAccessibleOrganizes(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/internal/organizes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        org.mockito.Mockito.verify(organizeLoadUseCase).getAccessibleOrganizes(null);
    }

    private static org.springframework.security.core.Authentication authenticatedUser() {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User("7", "", java.util.List.of()), "", java.util.List.of());
    }
}
