package io.jgitkins.server.presentation.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.server.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.application.port.in.OAuthLoginUseCase;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserStatus;
import io.jgitkins.server.presentation.dto.OAuthLoginRequest;
import io.jgitkins.server.presentation.mapper.OAuthRequestMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OAuthLoginUseCase oauthLoginUseCase;

    @MockBean
    private OAuthRequestMapper oauthRequestMapper;

    @Test
    void login_returnsWrappedTokenAndUser() throws Exception {
        OAuthLoginRequest request = new OAuthLoginRequest(
                "google",
                "sub-123",
                "user@example.com",
                "tester",
                true,
                "https://img.example.com/me.png"
        );
        OAuthLoginCommand command = new OAuthLoginCommand(
                "google",
                "sub-123",
                "user@example.com",
                "tester",
                true,
                "https://img.example.com/me.png"
        );
        User user = User.rehydrate(
                1L,
                "tester",
                "user@example.com",
                "tester",
                "https://img.example.com/me.png",
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        OAuthLoginResult result = new OAuthLoginResult("jwt-token", user, "google");

        when(oauthRequestMapper.toCommand(any(OAuthLoginRequest.class))).thenReturn(command);
        when(oauthLoginUseCase.login(command)).thenReturn(result);

        mockMvc.perform(post("/api/auth/oauth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.provider").value("google"))
                .andExpect(jsonPath("$.data.user.id").value(1L))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(oauthRequestMapper).toCommand(any(OAuthLoginRequest.class));
        verify(oauthLoginUseCase).login(command);
    }
}
