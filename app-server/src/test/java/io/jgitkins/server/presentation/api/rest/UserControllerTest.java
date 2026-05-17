package io.jgitkins.server.presentation.api.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jgitkins.server.application.dto.result.UserSummary;
import io.jgitkins.server.application.port.in.PublicUserQueryUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicUserQueryUseCase publicUserQueryUseCase;

    @Test
    void listUsers_returnsApiResponseWithUserSummaries() throws Exception {
        List<UserSummary> users = List.of(
                new UserSummary(1L, "alice", "Alice", "https://img/a.png", LocalDateTime.of(2026, 1, 1, 0, 0)),
                new UserSummary(2L, "bob", "Bob", null, LocalDateTime.of(2026, 1, 2, 0, 0)));
        when(publicUserQueryUseCase.getUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[1].username").value("bob"));

        verify(publicUserQueryUseCase).getUsers();
    }
}
