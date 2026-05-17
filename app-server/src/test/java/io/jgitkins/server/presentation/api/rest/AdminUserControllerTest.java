package io.jgitkins.server.presentation.api.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.result.UserAdminDetail;
import io.jgitkins.server.application.dto.result.UserAdminSummary;
import io.jgitkins.server.application.dto.result.UserIdentitySummary;
import io.jgitkins.server.application.port.in.AdminUserQueryUseCase;
import io.jgitkins.server.application.port.in.AdminUserUpdateUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserQueryUseCase adminUserQueryUseCase;

    @MockBean
    private AdminUserUpdateUseCase adminUserUpdateUseCase;

    @Test
    void listUsers_returnsAdminSummaries() throws Exception {
        when(adminUserQueryUseCase.getUsers()).thenReturn(List.of(
                new UserAdminSummary(1L, "admin", "a@b.com", "Admin", "ACTIVE", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        verify(adminUserQueryUseCase).getUsers();
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
        when(adminUserQueryUseCase.getUser(10L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/users/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.identities[0].providerName").value("google"));

        verify(adminUserQueryUseCase).getUser(10L);
    }

    @Test
    void updateStatus_callsUseCase() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("status", "BLOCKED"));

        mockMvc.perform(patch("/api/admin/users/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(adminUserUpdateUseCase).updateUserStatus(7L, "BLOCKED");
    }
}
