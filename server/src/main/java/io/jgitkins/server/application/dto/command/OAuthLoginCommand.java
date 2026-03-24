package io.jgitkins.server.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthLoginCommand {
    private final String provider;
    private final String subject;
    private final String email;
    private final String name;
    private final boolean emailVerified;
    private final String avatarUrl;
}
