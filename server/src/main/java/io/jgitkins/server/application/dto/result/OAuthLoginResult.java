package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.domain.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthLoginResult {
    private final String appToken;
    private final User user;
    private final String provider;
}
