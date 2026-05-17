package io.jgitkins.server.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.application.port.out.TokenIssuerPort;
import io.jgitkins.server.application.support.UserService;
import io.jgitkins.server.domain.model.User;
import io.jgitkins.server.domain.model.UserAuthority;
import io.jgitkins.server.domain.model.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthLoginServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenIssuerPort tokenIssuerPort;

    @InjectMocks
    private OAuthLoginService oAuthLoginService;

    @Test
    void login_createsOrLoadsUserAndReturnsResult() {
        OAuthLoginCommand command = new OAuthLoginCommand(
                "github",
                "sub-123",
                "user@github.com",
                "GH User",
                true,
                "https://img/avatar"
        );

        User user = User.rehydrate(
                1L,
                "ghuser",
                "user@github.com",
                "GH User",
                "https://img/avatar",
                UserAuthority.USER,
                UserStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.loginOrSignUp(any(UserLoginOrSignUpCommand.class))).thenReturn(user);
        when(tokenIssuerPort.issueToken(1L, List.of("ROLE_USER"))).thenReturn("app-token");

        OAuthLoginResult result = oAuthLoginService.login(command);

        assertEquals("app-token", result.getAppToken());
        assertEquals(user, result.getUser());
        assertEquals("github", result.getProvider());

        verify(userService).loginOrSignUp(any(UserLoginOrSignUpCommand.class));
        verify(tokenIssuerPort).issueToken(1L, List.of("ROLE_USER"));
    }
}
