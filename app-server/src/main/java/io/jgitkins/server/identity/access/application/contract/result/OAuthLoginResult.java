package io.jgitkins.server.identity.access.application.contract.result;

import io.jgitkins.server.identity.access.domain.aggregate.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthLoginResult {
    private final String appToken;
    private final User user;
    private final String provider;
}
