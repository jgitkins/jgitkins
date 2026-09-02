package io.jgitkins.server.identity.access.application.port.in;

import io.jgitkins.server.identity.access.application.contract.OAuthLoginCommand;
import io.jgitkins.server.identity.access.application.contract.OAuthLoginResult;

public interface OAuthLoginUseCase {
    OAuthLoginResult login(OAuthLoginCommand command);
}
