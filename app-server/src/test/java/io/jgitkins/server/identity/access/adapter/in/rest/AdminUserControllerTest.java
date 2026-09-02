package io.jgitkins.server.identity.access.adapter.in.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.identity.access.application.contract.result.UserAdminDetail;
import io.jgitkins.server.identity.access.application.contract.result.UserAdminSummary;
import io.jgitkins.server.identity.access.application.contract.result.UserIdentitySummary;
import io.jgitkins.server.identity.access.application.port.in.AdminUserQueryUseCase;
import io.jgitkins.server.identity.access.application.port.in.AdminUserUpdateUseCase;
import io.jgitkins.server.support.ErrorStatusMappingTestConfig;
import io.jgitkins.server.support.TestAuthentication;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
// Required, not decorative: the slice pulls in GlobalExceptionHandler as a @RestControllerAdvice
// but excludes its CompositeErrorHttpStatusMapper dependency, so the context fails to start without this.
@Import({ErrorStatusMappingTestConfig.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserQueryUseCase adminUserQueryUseCase;

    @MockBean
    private AdminUserUpdateUseCase adminUserUpdateUseCase;

    private static final String ADMIN_SUBJECT = "900";

    @BeforeEach
    void signIn() {
        // Every method here took no principal before 2026-08-28, so these tests passed while the
        // endpoints were reachable unauthenticated. The subject now has to be present.
        TestAuthentication.authenticateAs(ADMIN_SUBJECT);
    }

    @AfterEach
    void signOut() {
        TestAuthentication.clear();
    }

    @Test
    void updateStatus_rejectsAnAnonymousCallerWithoutReachingTheUseCase() throws Exception {
        TestAuthentication.clear();
        String body = objectMapper.writeValueAsString(java.util.Map.of("status", "BLOCKED"));

        mockMvc.perform(patch("/api/admin/users/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        // The status write must not be attempted. Setting an account to BLOCKED or DELETED is what
        // this endpoint let an unauthenticated caller do.
        org.mockito.Mockito.verifyNoInteractions(adminUserUpdateUseCase);
    }

    @Test
    void listUsers_rejectsAnAnonymousCallerWithoutReachingTheUseCase() throws Exception {
        TestAuthentication.clear();

        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());

        // The list carries every user's email address.
        org.mockito.Mockito.verifyNoInteractions(adminUserQueryUseCase);
    }

    @Test
    void listUsers_returnsAdminSummaries() throws Exception {
        when(adminUserQueryUseCase.getUsers(900L)).thenReturn(List.of(
                new UserAdminSummary(1L, "admin", "a@b.com", "Admin", "ACTIVE", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        verify(adminUserQueryUseCase).getUsers(900L);
    }

    @Test
    void getUser_returnsDetail() throws Exception {
        UserAdminDetail detail = new UserAdminDetail(
                10L,
                "tester",
                "tester@example.com",
                "Tester",
                "https://img/user.png",
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of(new UserIdentitySummary("google", "sub-1", "tester@example.com", true, "Tester", null))
        );
        when(adminUserQueryUseCase.getUser(900L, 10L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/users/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.identities[0].providerName").value("google"));

        verify(adminUserQueryUseCase).getUser(900L, 10L);
    }

    @Test
    void updateStatus_callsUseCase() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("status", "BLOCKED"));

        mockMvc.perform(patch("/api/admin/users/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(adminUserUpdateUseCase).updateUserStatus(900L, 7L, "BLOCKED");
    }
}
