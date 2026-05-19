package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.dto.command.UserLoginOrSignUpCommand;
import io.jgitkins.server.identity.access.application.dto.result.OAuthLoginResult;
import io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase;
import io.jgitkins.server.identity.access.application.port.out.TokenIssuerPort;
import io.jgitkins.server.identity.access.application.support.UserService;
import io.jgitkins.server.identity.access.domain.aggregate.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OAuthLoginService implements OAuthLoginUseCase {

    private final UserService userService;
    private final TokenIssuerPort tokenIssuerPort;

    @Override
    public OAuthLoginResult login(OAuthLoginCommand command) {
        User user = userService.loginOrSignUp(new UserLoginOrSignUpCommand(
                command.provider(),
                command.subject(),
                command.email(),
                command.emailVerified(),
                command.name(),
                command.avatarUrl()
        ));
        String appToken = tokenIssuerPort.issueToken(user.getId(), List.of("ROLE_USER"));
        return new OAuthLoginResult(appToken, user, command.provider());
    }
}
