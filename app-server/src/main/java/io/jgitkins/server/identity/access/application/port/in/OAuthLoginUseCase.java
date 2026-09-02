package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.contract.command.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.contract.result.OAuthLoginResult;

public interface OAuthLoginUseCase {
    OAuthLoginResult login(OAuthLoginCommand command);
}
