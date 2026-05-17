package io.jgitkins.server.application.dto.command;

public record OAuthLoginCommand(
        String provider,
        String subject,
        String email,
        String name,
        boolean emailVerified,
        String avatarUrl
) {
}
