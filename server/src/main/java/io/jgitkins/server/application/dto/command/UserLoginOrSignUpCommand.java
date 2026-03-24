package io.jgitkins.server.application.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLoginOrSignUpCommand {
    private final String providerName;
    private final String providerSub;
    private final String email;
    private final boolean emailVerified;
    private final String name;
    private final String avatarUrl;
}
