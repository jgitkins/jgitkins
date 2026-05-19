package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.dto.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.dto.result.OAuthLoginResult;

public interface OAuthLoginUseCase {
    OAuthLoginResult login(OAuthLoginCommand command);
}
