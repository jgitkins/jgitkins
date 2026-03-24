package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.application.dto.result.OAuthLoginResult;

public interface OAuthLoginUseCase {
    OAuthLoginResult login(OAuthLoginCommand command);
}
