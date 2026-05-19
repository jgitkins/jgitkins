package io.jgitkins.server.identity.access.application.dto.command;

public record OAuthLoginCommand(
        String provider,
        String subject,
        String email,
        String name,
        boolean emailVerified,
        String avatarUrl
) {
}
